package org.matsim.application.analysis.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

import java.util.List;
import java.util.Set;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

@CommandLine.Command(name = "congestion", description = "Calculates congestion indices and relative travel times.")
@CommandSpec(requireEvents = true, requireNetwork = true,
	produces = {"traffic_stats_by_link_daily.csv", "traffic_stats_by_link_and_hour.csv", "traffic_stats_by_road_type_daily.csv", "traffic_stats_by_road_type_and_hour.csv"}
)
public class TrafficAnalysis implements MATSimAppCommand {

	private final Logger log = LogManager.getLogger(TrafficAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TrafficAnalysis.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TrafficAnalysis.class);

	/**
	 * Sample size is used to compute actual flow capacity.
	 */
	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Mixin
	private ShpOptions shpOptions;

	@CommandLine.Option(names = "--transport-modes", description = "transport modes to analyze", split = ",")
	private Set<String> modes = Set.of(TransportMode.car, "freight");

	public static void main(String[] args) {
		new TrafficAnalysis().execute(args);
	}

	private static Table normalizeColumns(Table table) {
		for (Column<?> c : table.columns()) {
			int start = c.name().indexOf("[");
			int end = c.name().indexOf("]");

			if (start > -1 && end > -1) {
				c.setName(c.name().substring(start + 1, end));
			}

			if (c instanceof DoubleColumn d) {
				d.set(Selection.withRange(0, d.size()), d.multiply(1000).round().divide(1000));
			}
		}

		return table;
	}

	@Override
	public Integer call() throws Exception {

		Network network = filterNetwork();

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setAnalyzedModes(modes);
		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(86400);
		builder.setTimeslice(3600);

		TravelTimeCalculator travelTimes = builder.build();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 86400, network);

		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(travelTimes);
		manager.addHandler(volumes);

		manager.initProcessing();
		EventsUtils.readEvents(manager, input.getEventsPath());
		manager.finishProcessing();

		TrafficStatsCalculator calc = new TrafficStatsCalculator(network, travelTimes.getLinkTravelTimes());

		Table ds = createDataset(network, calc, volumes);

		// TODO: Avg must be weighted by link length, (and lanes) ?
		// 	"lane_km" also for the aggregate info table

		// TODO:
		// columns speed_performance_index, congestion_index, avg_speed, simulated_traffic_volume, road_capacity_utilization,

		// TODO: aggregates: (one file each)
		// per link and time bin

		// per link daily

		// per road type and time bin
		// + (all = additiomal road_type)

		// per road type daily

		// TODO: compute daily and hourly data separately?
		// can daily be  aggregated from hourly values easily?

		List<String> aggr = List.of("speed_performance_index", "congestion_index", "avg_speed", "simulated_traffic_volume", "road_capacity_utilization");

		Table daily = normalizeColumns(ds.summarize(aggr, mean).by("link_id"));

		daily.write().csv(output.getPath("traffic_stats_by_link_daily.csv").toFile());

		// Copy of table with all link links under one road_type
		Table copy = ds.copy();
		copy.stringColumn("road_type").set(Selection.withRange(0, ds.rowCount()), "all");
		copy.forEach(ds::append);

		// TODO: weighted mean
		Table perRoadTypeAndHour = ds.summarize(aggr, mean).by("road_type", "hour");
		normalizeColumns(perRoadTypeAndHour)
			.rejectColumns("simulated_traffic_volume")
			.sortOn("road_type", "hour")
			.write().csv(output.getPath("traffic_stats_by_road_type_and_hour.csv").toFile());

		// TODO: needs weighted mean
		Table perRoadType = normalizeColumns(ds.summarize(aggr, mean).by("road_type"));
		perRoadType.column("road_type").setName("Road Type");
		perRoadType.column("road_capacity_utilization").setName("Cap. Utilization");
		perRoadType.column("avg_speed").setName("Avg. Speed [km/h]");
		perRoadType.column("congestion_index").setName("Congestion Index");
		perRoadType.column("speed_performance_index").setName("Speed Performance Index");

		perRoadType
			.rejectColumns("simulated_traffic_volume")
			.sortOn("Road type")
			.write().csv(output.getPath("traffic_stats_by_road_type_daily.csv").toFile());


		return 0;
	}

	/**
	 * Create table with all disaggregated data.
	 */
	private Table createDataset(Network network, TrafficStatsCalculator calc, VolumesAnalyzer volumes) {

		Table all = Table.create(
			TextColumn.create("link_id"),
			IntColumn.create("hour"),
			StringColumn.create("road_type"),
			DoubleColumn.create("lane_km"),
			DoubleColumn.create("speed_performance_index"),
			DoubleColumn.create("congestion_index"),
			DoubleColumn.create("avg_speed"),
			DoubleColumn.create("simulated_traffic_volume"),
			DoubleColumn.create("road_capacity_utilization")
		);

		for (Link link : network.getLinks().values()) {

			double[] vol = volumes.getVolumesPerHourForLink(link.getId());

			for (int h = 0; h < 24; h += 1) {
				Row row = all.appendRow();

				row.setString("link_id", link.getId().toString());
				row.setInt("hour", h);
				row.setString("road_type", NetworkUtils.getHighwayType(link));
				row.setDouble("lane_km", (link.getLength() * link.getNumberOfLanes()) / 1000);

				row.setDouble("speed_performance_index", calc.getSpeedPerformanceIndex(link, h * 3600));
				row.setDouble("congestion_index", calc.getLinkCongestionIndex(link, h * 3600, (h + 1) * 3600));

				// as km/h
				row.setDouble("avg_speed", calc.getAvgSpeed(link, h * 3600) * 3.6);

				row.setDouble("simulated_traffic_volume", vol[h] / sample.getSample());

				double capacity = link.getCapacity() * sample.getSample();
				row.setDouble("road_capacity_utilization", vol[h] / capacity);
			}
		}

		return all;
	}

	private Network filterNetwork() {

		Network unfiltered = input.getNetwork();
		NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());

		// Must contain one of the analyzed modes
		manager.addLinkFilter(l -> l.getAllowedModes().stream().anyMatch(modes::contains));

		if (shpOptions.isDefined()) {
			Geometry geometry = shpOptions.getGeometry();
			manager.addLinkFilter(l -> geometry.covers(MGC.coord2Point(l.getCoord())));
		}

		return manager.applyFilters();
	}
}
