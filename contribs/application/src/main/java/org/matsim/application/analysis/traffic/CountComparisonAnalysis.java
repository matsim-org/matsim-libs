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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Arrays;
import java.util.Map;

@CommandLine.Command(name = "todo", description = "todo")
@CommandSpec(requireEvents = true, requireCounts = true, requireNetwork = true, produces = {"count_comparison_by_hour.csv", "count_comparison_total.csv"})
public class CountComparisonAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Option(names = "--sample-size", description = "Sample size of scenario", defaultValue = "0.25")
	private double sampleSize;

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

	private void writeOutput(Counts<Link> counts, Network network, VolumesAnalyzer volumes) {

		Map<Id<Link>, ? extends Link> links = network.getLinks();

		Table byHour = Table.create(
				StringColumn.create("link_id"),
				StringColumn.create("name"),
				StringColumn.create("road_type"),
				DoubleColumn.create("hour"),
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

			if (type == null || type.isBlank())
				type = "unclassified";

			if (countVolume.isEmpty())
				continue;

			int[] volumesForLink = volumes.getVolumesForLink(key, TransportMode.car);

			if (countVolume.isEmpty())
				continue;

			if (countVolume.size() == 24) {

				for (int hour = 1; hour < 25; hour++) {

					double observedTrafficVolumeAtHour = countVolume.get(hour).getValue();
					double simulatedTrafficVolumeAtHour = volumesForLink == null ? 0.0 :
							((double) volumesForLink[hour - 1]) / sampleSize;

					Row row = byHour.appendRow();
					row.setString("link_id", key.toString());
					row.setString("name", name);
					row.setString("road_type", type);
					row.setDouble("hour", hour);
					row.setDouble("observed_traffic_volume", observedTrafficVolumeAtHour);
					row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeAtHour);
				}
			}

			double observedTrafficVolumeByDay = countVolume.values().stream().map(Volume::getValue).reduce(Double::sum).orElse(0.0);
			double simulatedTrafficVolumeByDay = volumesForLink != null ? Arrays.stream(volumesForLink).sum() : 0.0;

			Row row = dailyTrafficVolume.appendRow();
			row.setString("link_id", key.toString());
			row.setString("name", name);
			row.setString("road_type", type);
			row.setDouble("observed_traffic_volume", observedTrafficVolumeByDay);
			row.setDouble("simulated_traffic_volume", simulatedTrafficVolumeByDay);

			dailyTrafficVolume.write().csv(output.getPath("count_comparison_total.csv").toFile());
			byHour.write().csv(output.getPath("count_comparison_by_hour.csv").toFile());
		}
	}
}
