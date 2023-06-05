package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

import java.util.Set;

@CommandLine.Command(name = "congestion", description = "Calculates congestion indices and relative traveltimes.")
@CommandSpec(requireEvents = true, requireNetwork = true,
		produces = {"traffic_stats_by_link_daily.csv", "traffic_stats_by_link_and_hour.csv", "traffic_stats_by_road_type_daily.csv", "traffic_stats_by_road_type_and_hour.csv"}
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

		if (this.timeSlice <= 900) {
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

		EventsManager manager = EventsUtils.createParallelEventsManager();
		VolumesAnalyzer analyzer = new VolumesAnalyzer(timeSlice, maxTime, network);

		manager.addHandler(analyzer);
		manager.initProcessing();

		EventsUtils.readEvents(manager, input.getEventsPath());

		Table byLinkAndTime = Table.create(DoubleColumn.create("time"), StringColumn.create("road_type"), DoubleColumn.create("congestion_index"),
				DoubleColumn.create("speed_performance_index"), DoubleColumn.create("avg_speed"), DoubleColumn.create("simulated_traffic_volume"),
				DoubleColumn.create("road_capacity_utilization"));

		Table byTimeAndRoadType = Table.create(DoubleColumn.create("time"), StringColumn.create("road_type"), DoubleColumn.create("congestion_index"),
				DoubleColumn.create("avg_speed"), DoubleColumn.create("simulated_traffic_volume"), DoubleColumn.create("road_capacity_utilization"));

		// TODO:
		// columns speed_performance_index, congestion_index, avg_speed, simulated_traffic_volume, road_capacity_utilization,

		// TODO: aggregates: (one file each)
		// per link and time bin

		// per link daily

		// per road type and time bin
		// + (all = additiomal road_type)

		// per road type daily

		for (Link link : network.getLinks().values()) {

			double capacity = link.getFlowCapacityPerSec();

			/*int[] volumes = analyzer.getVolumesForLink(link.getId());
			int arrayCounter = 0;*/

			for (int i = startTime; i < maxTime; i += timeSlice) {

				/*int simulatedTrafficVolume = volumes[arrayCounter++];
				double capacityForPeriod = capacity * timeSlice;

				double roadUtilization = (double) simulatedTrafficVolume / capacityForPeriod;*/

				double speedPerformanceIndex = congestionIndex.getSpeedPerformanceIndex(link, i);
				double linkCongestionIndex = congestionIndex.getLinkCongestionIndex(link, i, i + timeSlice);
				double avgSpeed = congestionIndex.getAvgSpeed(link, i, i + timeSlice);

				String type = NetworkUtils.getHighwayType(link);

				Row row = byLinkAndTime.appendRow();

				row.setDouble("time", i);
				row.setString("road_type", type);
				row.setDouble("congestion_index", linkCongestionIndex);
				row.setDouble("avg_speed", avgSpeed);
				/*row.setDouble("simulated_traffic_volume", simulatedTrafficVolume);
				row.setDouble("speed_performance_index", speedPerformanceIndex);
				row.setDouble("road_capacity_utilization", roadUtilization);*/
			}
		}

		for (int i = startTime; i < maxTime; i += timeSlice) {

			double overallIndex = congestionIndex.getNetworkCongestionIndex(i, i + timeSlice, null);

			Row row = byTimeAndRoadType.appendRow();
			row.setDouble("time", i);
			row.setString("road_type", "all");
			row.setDouble("congestion_index", overallIndex);

			for (String type : roadTypes) {

				Row rowForRoadType = byTimeAndRoadType.appendRow();

				double indexForRoadType = congestionIndex.getNetworkCongestionIndex(i, i + timeSlice, type);
				row.setDouble("time", i);
				rowForRoadType.setString("road_type", type);
				rowForRoadType.setDouble("congestion_index", indexForRoadType);
			}
		}

		//TODO merge both for loops

		Table byRoadType = byTimeAndRoadType.summarize("congestion_index", mean).by("road_type");
		byRoadType.column("Mean [congestion_index]").setName("congestion_index");

		byTimeAndRoadType.write().csv(output.getPath("traffic_stats_by_road_type_and_hour.csv").toFile());
		byRoadType.write().csv(output.getPath("traffic_stats_by_road_type_daily.csv").toFile());

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
