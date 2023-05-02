package org.matsim.application.analysis.traffic;

import org.apache.commons.csv.CSVPrinter;
import org.matsim.analysis.VolumesAnalyzer;
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
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "todo", description = "todo")
@CommandSpec(requireEvents = true, requireCounts = true, requireNetwork = true, produces = {"count_comparison.csv"})
public class CountComparisonAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(StuckAgentAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(StuckAgentAnalysis.class);

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

		try (CSVPrinter printer = csv.createPrinter(output.getPath("count_comparison.csv"))) {

			printer.printRecord("link_id", "name", "road_type", "observed_traffic_volume", "simulated_traffic_volume");

//			NetworkUtils.getType()

		}


	}


}
