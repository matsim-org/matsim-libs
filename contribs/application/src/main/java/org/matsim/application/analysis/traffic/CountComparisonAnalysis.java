package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.*;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.selection.Selection;

import java.nio.file.Path;
import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.count;
import static tech.tablesaw.aggregate.AggregateFunctions.mean;

@CommandLine.Command(name = "count-comparison", description = "Produces comparisons of observed and simulated counts.")
@CommandSpec(requireEvents = true, requireCounts = true, requireNetwork = true,
	produces = {"count_comparison_by_hour.csv", "count_comparison_daily.csv", "count_comparison_quality.csv", "count_error_by_hour.csv"})
public class CountComparisonAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Option(names = "--limits", split = ",", description = "Limits for quality label categories", defaultValue = "0.6,0.8,1.2,1.4")
	private List<Double> limits;

	@CommandLine.Option(names = "--labels", split = ",", description = "Labels for quality categories", defaultValue = "major under,under,ok,over,major over")
	private List<String> labels;

	@CommandLine.Option(names = {"--network-mode", "--transport-mode"}, description = "Mode(s) to analyze", split = ",", defaultValue = TransportMode.car)
	private Set<String> modes;

	public static void main(String[] args) {
		new CountComparisonAnalysis().execute(args);
	}

	private static String cut(double relError, List<Double> errorGroups, List<String> labels) {

		int idx = Collections.binarySearch(errorGroups, relError);

		if (idx >= 0)
			return labels.get(idx);

		int ins = -(idx + 1);
		return labels.get(ins);
	}

	/**
	 * Sum two arrays element-wise into new array.
	 */
	private static int[] sum(int[] a, int[] b) {
		int[] counts = new int[a.length];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = a[i] + b[i];
		}

		return counts;
	}

	/**
	 * Calculate the geh value for simulated and reference count
	 */
	private static double geh(double simulated, double observed) {
		final double diff = simulated - observed;
		final double sum = simulated + observed;

		return Math.sqrt(2 * diff * diff / sum);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		Network network = input.getNetwork();

		VolumesAnalyzer volume = new VolumesAnalyzer(3600, 86400, network, true);

		eventsManager.addHandler(volume);

		eventsManager.initProcessing();

		EventsUtils.readEvents(eventsManager, input.getEventsPath());

		eventsManager.finishProcessing();

		Counts<Link> counts = new Counts<>();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.readFile(input.getCountsPath());

		Table byHour = writeOutput(counts, network, volume);

		writeErrorMetrics(byHour, output.getPath("count_error_by_hour.csv"));

		return 0;
	}

	private Table writeOutput(Counts<Link> counts, Network network, VolumesAnalyzer volumes) {

		Map<Id<Link>, ? extends Link> links = network.getLinks();

		Table byHour = Table.create(
			StringColumn.create("link_id"),
			StringColumn.create("name"),
			StringColumn.create("road_type"),
			IntColumn.create("hour"),
			DoubleColumn.create("observed_traffic_volume"),
			DoubleColumn.create("simulated_traffic_volume"),
			DoubleColumn.create("geh")
		);

		Table dailyTrafficVolume = Table.create(StringColumn.create("link_id"),
			StringColumn.create("name"),
			StringColumn.create("road_type"),
			DoubleColumn.create("observed_traffic_volume"),
			DoubleColumn.create("simulated_traffic_volume"),
			DoubleColumn.create("geh")
		);

		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> entry : counts.getMeasureLocations().entrySet()) {
			Id<Link> key = entry.getKey();

			Int2DoubleMap countVolume =  aggregateObserved(entry.getValue(), modes);

			String name = entry.getValue().getDisplayName();

			Link link = links.get(key);
			String type = NetworkUtils.getHighwayType(link);

			if (countVolume.isEmpty())
				continue;

			Optional<int[]> opt = modes.stream()
				.map(mode -> volumes.getVolumesForLink(key, mode))
				.filter(Objects::nonNull)
				.reduce(CountComparisonAnalysis::sum);

			int[] volumesForLink;
			if (countVolume.isEmpty() || opt.isEmpty()) {
				volumesForLink = new int[24];
			} else {
				volumesForLink = opt.get();
			}

			double simulatedTrafficVolumeByDay = 0;
			double observedTrafficVolumeByDay = 0;

			// Hourly values are present
			if (countVolume.size() > 1 || !countVolume.containsKey(24)) {

				for (int hour = 0; hour < 24; hour++) {

					double observedTrafficVolumeAtHour = countVolume.get(hour);
					double simulatedTrafficVolumeAtHour = (double) volumesForLink[hour] / this.sample.getSample();

					simulatedTrafficVolumeByDay += simulatedTrafficVolumeAtHour;
					observedTrafficVolumeByDay += observedTrafficVolumeAtHour;

					Row row = byHour.appendRow();
					row.setString("link_id", key.toString());
					row.setString("name", name);
					row.setString("road_type", type);
					row.setInt("hour", hour);
					row.setDouble("observed_traffic_volume", observedTrafficVolumeAtHour);
					row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeAtHour);
					row.setDouble("geh", geh(simulatedTrafficVolumeAtHour, observedTrafficVolumeAtHour));
				}
			} else {
				// Get the daily values
				observedTrafficVolumeByDay = countVolume.get(24);
				simulatedTrafficVolumeByDay = Arrays.stream(volumesForLink).sum() / this.sample.getSample();
			}

			Row row = dailyTrafficVolume.appendRow();
			row.setString("link_id", key.toString());
			row.setString("name", name);
			row.setString("road_type", type);
			row.setDouble("observed_traffic_volume", observedTrafficVolumeByDay);
			row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeByDay);
			row.setDouble("geh", geh(simulatedTrafficVolumeByDay, observedTrafficVolumeByDay));
		}

		DoubleColumn relError = dailyTrafficVolume.doubleColumn("simulated_traffic_volume")
			.divide(dailyTrafficVolume.doubleColumn("observed_traffic_volume"))
			.setName("rel_error");

		StringColumn qualityLabel = relError.copy()
			.map(err -> cut(err, limits, labels), ColumnType.STRING::create)
			.setName("quality");

		dailyTrafficVolume.addColumns(relError, qualityLabel);


		dailyTrafficVolume = dailyTrafficVolume.sortOn("road_type", "link_id");
		dailyTrafficVolume.write().csv(output.getPath("count_comparison_daily.csv").toFile());

		byHour = byHour.sortOn("name");
		byHour.write().csv(output.getPath("count_comparison_by_hour.csv").toFile());

		Table byQuality = dailyTrafficVolume.summarize("quality", count).by("quality", "road_type");
		byQuality.column(2).setName("n");

		// Sort by quality
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString("quality")));

		byQuality = byQuality.sortOn(cmp.thenComparing(row -> row.getNumber("n")));
		byQuality.addColumns(byQuality.doubleColumn("n").copy().setName("share"));

		List<String> roadTypes = byQuality.stringColumn("road_type").unique().asList();

		// Norm within each road type
		for (String roadType : roadTypes) {
			DoubleColumn share = byQuality.doubleColumn("share");
			Selection sel = byQuality.stringColumn("road_type").isEqualTo(roadType);

			double total = share.where(sel).sum();
			if (total > 0)
				share.set(sel, share.divide(total));
		}

		byQuality.write().csv(output.getPath("count_comparison_quality.csv").toFile());

		return byHour;
	}

	/**
	 * Aggregate observed counts by hour. Starting at 0.
	 */
	private static Int2DoubleMap aggregateObserved(MeasurementLocation<Link> m, Set<String> modes) {

		Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();

		for (String mode : modes) {
			Measurable volumes = m.getVolumesForMode(mode);
			if (volumes == null)
				continue;

			if (volumes.supportsHourlyAggregate()) {
				for (int i = 0; i < 24; i++) {
					OptionalDouble v = volumes.aggregateAtHour(i);
					if (v.isPresent())
						map.mergeDouble(i, v.getAsDouble(), Double::sum);
				}
			} else {
				// Daily value is stored under separate key
				map.mergeDouble(24, volumes.aggregateDaily(), Double::sum);
			}
		}

		return map;
	}

	private void writeErrorMetrics(Table byHour, Path path) {

		byHour.addColumns(
			byHour.doubleColumn("simulated_traffic_volume").subtract(byHour.doubleColumn("observed_traffic_volume")).setName("error")
		);

		byHour.addColumns(
			byHour.doubleColumn("error").abs().setName("abs_error")
		);

		DoubleColumn relError = byHour.doubleColumn("abs_error")
			.multiply(100)
			.divide(byHour.doubleColumn("observed_traffic_volume"))
			.setName("rel_error");

		// Cut-off at Max error
		relError = relError.set(relError.isMissing(), 1000d);
		relError = relError.map(d -> Math.min(d, 1000d));

		byHour.addColumns(relError);

		Table aggr = byHour.summarize("error", "abs_error", "rel_error", mean).by("hour");

		aggr.column("Mean [error]").setName("mean_bias");
		aggr.column("Mean [rel_error]").setName("mean_rel_error");
		aggr.column("Mean [abs_error]").setName("mean_abs_error");

		aggr.write().csv(path.toFile());
	}

}
