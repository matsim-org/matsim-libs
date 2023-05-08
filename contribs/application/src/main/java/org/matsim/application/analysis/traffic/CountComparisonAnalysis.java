package org.matsim.application.analysis.traffic;

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
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import picocli.CommandLine;
import tech.tablesaw.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "count-comparison", description = "Produces comparisons of observed and simulated counts.")
@CommandSpec(requireEvents = true, requireCounts = true, requireNetwork = true,
	produces = {"count_comparison_by_hour.csv", "count_comparison_total.csv"})
public class CountComparisonAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private SampleOptions sample;

	public static void main(String[] args) {
		new CountComparisonAnalysis().execute(args);
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

		writeOutput(counts, network, volume);

		return 0;
	}

	private static String cut(double relError, List<Double> errorGroups, List<String> labels) {

		int idx = Collections.binarySearch(errorGroups, relError);

		if (idx >= 0)
			return labels.get(idx);

		int ins = -(idx + 1);
		return labels.get(ins - 1);
	}

	private void writeOutput(Counts<Link> counts, Network network, VolumesAnalyzer volumes) {

		Map<Id<Link>, ? extends Link> links = network.getLinks();

		Table byHour = Table.create(
			StringColumn.create("link_id"),
			StringColumn.create("name"),
			StringColumn.create("road_type"),
			IntColumn.create("hour"),
			DoubleColumn.create("observed_traffic_volume"),
			DoubleColumn.create("simulated_traffic_volume")
		);

		Table dailyTrafficVolume = Table.create(StringColumn.create("link_id"),
			StringColumn.create("name"),
			StringColumn.create("road_type"),
			DoubleColumn.create("observed_traffic_volume"),
			DoubleColumn.create("simulated_traffic_volume")
		);

		for (Map.Entry<Id<Link>, Count<Link>> entry : counts.getCounts().entrySet()) {
			Id<Link> key = entry.getKey();
			Map<Integer, Volume> countVolume = entry.getValue().getVolumes();
			String name = entry.getValue().getCsLabel();

			Link link = links.get(key);
			String type = NetworkUtils.getType(link);

			if (type != null)
				type = type.replaceFirst("^highway\\.", "");

			if (type == null || type.isBlank())
				type = "unclassified";

			if (countVolume.isEmpty())
				continue;

			int[] volumesForLink = volumes.getVolumesForLink(key, TransportMode.car);

			if (countVolume.isEmpty())
				continue;

			if (countVolume.size() == 24) {

				// FIXME : why does it start at hour 1 ?

				for (int hour = 1; hour < 25; hour++) {

					double observedTrafficVolumeAtHour = countVolume.get(hour).getValue();
					double simulatedTrafficVolumeAtHour = volumesForLink == null ? 0.0 :
						((double) volumesForLink[hour - 1]) / sample.getSample();

					Row row = byHour.appendRow();
					row.setString("link_id", key.toString());
					row.setString("name", name);
					row.setString("road_type", type);
					row.setInt("hour", hour);
					row.setDouble("observed_traffic_volume", observedTrafficVolumeAtHour);
					row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeAtHour);
				}
			}

			double observedTrafficVolumeByDay = countVolume.values().stream().mapToDouble(Volume::getValue).sum();
			double simulatedTrafficVolumeByDay = volumesForLink != null ? Arrays.stream(volumesForLink).sum() : 0.0;

			Row row = dailyTrafficVolume.appendRow();
			row.setString("link_id", key.toString());
			row.setString("name", name);
			row.setString("road_type", type);
			row.setDouble("observed_traffic_volume", observedTrafficVolumeByDay);
			row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeByDay);
		}

		// TODO: add the classification as another column to count comparison
		// major under, under, exact, over, major over

		DoubleColumn relError = dailyTrafficVolume.doubleColumn("simulated_traffic_volume")
			.divide(dailyTrafficVolume.doubleColumn("observed_traffic_volume"))
			.setName("rel_error");

		StringColumn qualityLabel = relError.copy()
				.map(aDouble -> cut(aDouble, List.of(0.0, 0.8, 1.2, Double.MAX_VALUE), List.of("major under", "under", "exact", "over", "major over")), ColumnType.STRING::create)
				.setName("quality");

		dailyTrafficVolume.addColumns(relError, qualityLabel);

		dailyTrafficVolume.write().csv(output.getPath("count_comparison_total.csv").toFile());
		byHour.write().csv(output.getPath("count_comparison_by_hour.csv").toFile());
	}
}
