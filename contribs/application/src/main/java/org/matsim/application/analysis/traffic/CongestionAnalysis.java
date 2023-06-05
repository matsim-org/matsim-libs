package org.matsim.application.analysis.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Set;

@CommandLine.Command(name = "congestion", description = "Calculates congestion indices and relative traveltimes.")
@CommandSpec(requireEvents = true, requireNetwork = true,
		produces = {"network_congestion_total.csv", "network_congestion_by_road_type.csv"}
)
public class CongestionAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	InputOptions input = InputOptions.ofCommand(CongestionAnalysis.class);

	@CommandLine.Mixin
	OutputOptions output = OutputOptions.ofCommand(CongestionAnalysis.class);

	@CommandLine.Option(names = "--transport-modes", description = "transport modes to analyze")
	Set<String> modes = Set.of(TransportMode.car, "freight");

	@CommandLine.Option(names = "--road-types", description = "road types to consider")
	Set<String> roadTypes = Set.of("trunk", "motorway", "primary", "secondary", "tertiary", "residential", "unclassified");

	@CommandLine.Mixin
	ShpOptions shpOptions;

	@CommandLine.Option(names = "--start-time", description = "first daytime in s to start the analysis", defaultValue = "0")
	private int startTime;

	@CommandLine.Option(names = "--max-time", description = "max daytime for travel time analysis", defaultValue = "86400")
	private int maxTime;

	@CommandLine.Option(names = "--time-slice", description = "time bin to aggregate travel times and indices", defaultValue = "900")
	private int timeSlice;

	private final Logger logger = LogManager.getLogger(CongestionAnalysis.class);

	@Override
	public Integer call() throws Exception {

		if(this.timeSlice <= 900){
			this.timeSlice = 900;
			logger.info("Set time slice to minimum value of 900");
		}

		Network network = filterNetwork();

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setAnalyzedModes(modes);
		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(maxTime);
		builder.setTimeslice(timeSlice);

		TravelTime travelTimes = builder.build().getLinkTravelTimes();

		CongestionIndex congestionIndex = new CongestionIndex(network, travelTimes);

		Table table = Table.create(DoubleColumn.create("time"), DoubleColumn.create("congestion_index"));

		Table byRoadType = table.copy().addColumns(StringColumn.create("road_type"));

		for (int i = startTime; i < maxTime; i += timeSlice) {
			double overallIndex = congestionIndex.getNetworkCongestionIndex(i, i + timeSlice, null);

			Row row = table.appendRow();
			row.setDouble("time", i);
			row.setDouble("congestion_index", overallIndex);

			Row detailedRow = byRoadType.appendRow();
			detailedRow.setDouble("time", i);

			for (String type : roadTypes) {

				double indexForRoadType = congestionIndex.getNetworkCongestionIndex(i, i + timeSlice, type);
				detailedRow.setString("road_type", type);
				detailedRow.setDouble("congestion_index", indexForRoadType);
			}
		}

		table.write().csv(output.getPath("network_congestion_total.csv").toFile());
		byRoadType.write().csv(output.getPath("network_congestion_by_road_type.csv").toFile());

		return 0;
	}

	private Network filterNetwork() {

		Network unfiltered = input.getNetwork();
		NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());
		manager.addLinkFilter(l -> !l.getId().toString().startsWith("pt_"));

		if (shpOptions.isDefined()) {

			Geometry geometry = shpOptions.getGeometry();
			manager.addLinkFilter(l -> geometry.covers(MGC.coord2Point(l.getCoord())));
		}

		return manager.applyFilters();
	}
}
