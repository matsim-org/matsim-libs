package org.matsim.application.analysis.traffic;

import org.matsim.analysis.VolumesAnalyzer;
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
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;
import static tech.tablesaw.aggregate.AggregateFunctions.sum;

@CommandLine.Command(name = "congestion", description = "Calculates congestion indices and relative travel times.")
@CommandSpec(requireEvents = true, requireNetwork = true,
	produces = {"traffic_stats_by_link_daily.csv", "traffic_stats_by_link_and_hour.csv", "traffic_stats_by_road_type_daily.csv", "traffic_stats_by_road_type_and_hour.csv"}
)
public class TrafficAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TrafficAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TrafficAnalysis.class);

	/**
	 * Sample size is used to compute actual flow capacity.
	 */
	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--transport-modes", description = "transport modes to analyze", defaultValue = "", split = ",")
	private Set<String> modes;

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
		}

		return table;
	}

	@Override
	public Integer call() throws Exception {

		Network network = filterNetwork();

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		if (modes != null && !modes.isEmpty()) {
			builder.setFilterModes(true);
			builder.setAnalyzedModes(modes);
		}

		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(86400);
		builder.setTimeslice(900);

		TravelTimeCalculator travelTimes = builder.build();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 86400, network, true);

		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(travelTimes);
		manager.addHandler(volumes);

		manager.initProcessing();
		EventsUtils.readEvents(manager, input.getEventsPath());
		manager.finishProcessing();

		TrafficStatsCalculator calc = new TrafficStatsCalculator(network, travelTimes.getLinkTravelTimes(), 900);

		Table ds = createDataset(network, calc, volumes);

		List<String> means = List.of("speed_performance_index", "congestion_index", "avg_speed", "road_capacity_utilization", "lane_km");
		Table dailyMean = normalizeColumns(ds.summarize(means, mean).by("link_id"));

		List<String> sums = ds.columnNames().stream().filter(s -> s.startsWith("vol_") || s.endsWith("_volume")).toList();
		Table dailySum = normalizeColumns(ds.summarize(sums, sum).by("link_id"));

		Table daily = dailyMean.joinOn("link_id").inner(dailySum);

		daily.write().csv(output.getPath("traffic_stats_by_link_daily.csv").toFile());

		// Copy of table with all link links under one road_type
		Table copy = ds.copy();
		copy.stringColumn("road_type").set(Selection.withRange(0, ds.rowCount()), "all");
		copy.forEach(ds::append);

		Table perRoadTypeAndHour = Table.create(StringColumn.create("road_type"), IntColumn.create("hour"), DoubleColumn.create("congestion_index"));
		Set<String> roadTypes = new HashSet<>(ds.stringColumn("road_type").asList());

		for (int hour = 0; hour < 24; hour++) {

			for (String roadType : roadTypes) {

				double congestionIndex = calc.getNetworkCongestionIndex(hour * 3600, (hour + 1) * 3600, roadType.equals("all") ? null : roadType);

				Row row = perRoadTypeAndHour.appendRow();
				row.setString("road_type", roadType);
				row.setInt("hour", hour);
				row.setDouble("congestion_index", congestionIndex);
			}
		}

		perRoadTypeAndHour
			.sortOn("road_type", "hour")
			.write().csv(output.getPath("traffic_stats_by_road_type_and_hour.csv").toFile());

		Table dailyCongestionIndex = Table.create(StringColumn.create("road_type"), DoubleColumn.create("congestion_index"));

		for (String roadType : roadTypes) {

			double congestionIndex = calc.getNetworkCongestionIndex(0, 86400, roadType.equals("all") ? null : roadType);
			Row row = dailyCongestionIndex.appendRow();
			row.setString("road_type", roadType);
			row.setDouble("congestion_index", congestionIndex);
		}

		Table perRoadType = dailyCongestionIndex.joinOn("road_type").leftOuter(
			weightedMeanBy(ds, means, "road_type").rejectColumns("speed_performance_index", "congestion_index")
		);

		DoubleColumn meanLaneKm = perRoadType.doubleColumn("lane_km").divide(24).multiply(1000).round().divide(1000).setName("lane_km");
		perRoadType.replaceColumn(meanLaneKm);

		perRoadType.column("lane_km").setName("Total lane km");
		perRoadType.column("road_type").setName("Road Type");
		perRoadType.column("road_capacity_utilization").setName("Cap. Utilization");
		perRoadType.column("avg_speed").setName("Avg. Speed [km/h]");
		perRoadType.column("congestion_index").setName("Congestion Index");

		roundColumns(perRoadType);
		perRoadType
			.sortOn("Road Type")
			.write().csv(output.getPath("traffic_stats_by_road_type_daily.csv").toFile());

		return 0;
	}

	private Table weightedMeanBy(Table table, List<String> aggr, String... by) {
		Table first = multiplyWithLinkLength(table).summarize(aggr, sum).by(by);
		return divideByLength(normalizeColumns(first));
	}

	private Table divideByLength(Table table) {

		Table copy = Table.create();
		for (Column<?> column : table.columns()) {

			if (column instanceof DoubleColumn d && !column.name().equals("lane_km")) {
				String name = column.name();
				DoubleColumn divided = d.divide(table.doubleColumn("lane_km")).setName(name);
				copy.addColumns(divided);
			} else
				copy.addColumns(column);
		}

		return copy;
	}

	private Table multiplyWithLinkLength(Table table) {

		Table copy = Table.create();

		for (Column<?> column : table.columns()) {

			if (column instanceof DoubleColumn d && !column.name().equals("lane_km")) {
				DoubleColumn multiplied = d.multiply(table.doubleColumn("lane_km")).setName(column.name());
				copy.addColumns(multiplied);
			} else
				copy.addColumns(column);
		}
		return copy;
	}

	private void roundColumns(Table table) {

		for (Column<?> column : table.columns()) {
			if (column instanceof DoubleColumn d) {
				d.set(Selection.withRange(0, d.size()), d.multiply(1000).round().divide(1000));
			}
		}
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
			DoubleColumn.create("road_capacity_utilization"),
			DoubleColumn.create("simulated_traffic_volume")
		);

		// Somehow Expensive operation
		Set<String> modes = volumes.getModes();

		for (String mode : modes) {
			all.addColumns(DoubleColumn.create("vol_" + mode));
		}

		for (Link link : network.getLinks().values()) {

			double[] vol = volumes.getVolumesPerHourForLink(link.getId());

			for (int h = 0; h < 24; h += 1) {
				Row row = all.appendRow();

				row.setString("link_id", link.getId().toString());
				row.setInt("hour", h);
				row.setString("road_type", NetworkUtils.getHighwayType(link));
				row.setDouble("lane_km", (link.getLength() * link.getNumberOfLanes()) / 1000);

				int startTime = h * 3600;
				int endTime = (h + 1) * 3600;

				row.setDouble("speed_performance_index", calc.getSpeedPerformanceIndex(link, startTime, endTime));
				row.setDouble("congestion_index", calc.getLinkCongestionIndex(link, startTime, endTime));

				// as km/h
				row.setDouble("avg_speed", calc.getAvgSpeed(link, startTime, endTime) * 3.6);

				double capacity = link.getCapacity() * sample.getSample();
				row.setDouble("road_capacity_utilization", vol[h] / capacity);

				row.setDouble("simulated_traffic_volume", vol[h] / sample.getSample());

				for (String mode : modes) {
					row.setDouble("vol_" + mode, volumes.getVolumesPerHourForLink(link.getId(), mode)[h] / sample.getSample());
				}
			}
		}

		return all;
	}

	private Network filterNetwork() {

		Network unfiltered = input.getNetwork();
		NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());

		// Must contain one of the analyzed modes
		manager.addLinkFilter(l -> l.getAllowedModes().stream().anyMatch(s -> modes.contains(s)));

		if (shp.isDefined()) {
			String crs = ProjectionUtils.getCRS(unfiltered);
			ShpOptions.Index index = shp.createIndex(crs != null ? crs : shp.getShapeCrs(), "_");
			manager.addLinkFilter(l -> index.contains(l.getCoord()));
		}

		return manager.applyFilters();
	}
}
