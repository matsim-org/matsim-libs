package org.matsim.application.analysis.traffic;

import org.apache.commons.csv.CSVPrinter;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.application.options.CsvOptions;
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

import java.io.IOException;
import java.util.Map;

@CommandLine.Command(name = "todo", description = "todo")
@CommandSpec(requireEvents = true, requireCounts = true, requireNetwork = true, produces = {"count_comparison.csv"})
public class CountComparisonAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CountComparisonAnalysis.class);

	@CommandLine.Mixin
	private CsvOptions csv;

	public static void main(String[] args) {
		new CountComparisonAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		Network network = input.getNetwork();

		VolumesAnalyzer volume = new VolumesAnalyzer(86400, 86400, network, true);

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

	private void writeOutput(Counts<Link> counts, Network network, VolumesAnalyzer volumes) throws IOException {

		Map<Id<Link>, ? extends Link> links = network.getLinks();

		try (CSVPrinter printer = csv.createPrinter(output.getPath("count_comparison.csv"))) {

			printer.printRecord("link_id", "name", "road_type", "observed_traffic_volume", "simulated_traffic_volume");

			for (Map.Entry<Id<Link>, Count<Link>> entry : counts.getCounts().entrySet()) {
				Id<Link> key = entry.getKey();
				Map<Integer, Volume> countVolume = entry.getValue().getVolumes();
				String name = entry.getValue().getCsLabel();

				if (countVolume.isEmpty())
					continue;

				Double observedTrafficVolume = countVolume.values().stream().map(Volume::getValue).reduce(Double::sum).get();
				int[] volumesForLink = volumes.getVolumesForLink(key, TransportMode.car);

				Integer simulatedTrafficVolume;

				if (volumesForLink == null) {
					simulatedTrafficVolume = 0;
				} else {
					simulatedTrafficVolume = volumesForLink[0];
				}

				Link link = links.get(key);
				String type = NetworkUtils.getType(link);

				printer.printRecord(key, name, type, observedTrafficVolume, simulatedTrafficVolume);
			}
		}
	}
}
