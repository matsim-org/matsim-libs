package org.matsim.simwrapper.analysis;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "stuck-agents", description = "Generates statistics for stuck agents.")
@CommandSpec(requireEvents = true, produces = {"stuckAgentsPerHour.csv", "stuckAgentsPerMode.csv", "stuckAgentsPerLink.csv", "stuckAgentsPerModePieChart.csv", "header.md"})
public class StuckAgentAnalysis implements MATSimAppCommand, PersonStuckEventHandler, ActivityStartEventHandler {
	private static final Logger log = LogManager.getLogger(StuckAgentAnalysis.class);
	private final Object2IntMap<String> stuckAgentsPerMode = new Object2IntOpenHashMap<>();
	private final Map<String, Int2DoubleMap> stuckAgentsPerHour = new HashMap<>();
	private final Map<String, Object2DoubleOpenHashMap<String>> stuckAgentsPerLink = new HashMap<>();
	private final Object2DoubleOpenHashMap<String> allStuckedLinks = new Object2DoubleOpenHashMap<>();
	private final List<String> allAgents = new ArrayList<>();
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(StuckAgentAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(StuckAgentAnalysis.class);
	private int maxHour = 0;

	public static void main(String[] args) {
		new StuckAgentAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);

		manager.initProcessing();

		EventsUtils.readEvents(manager, input.getEventsPath());

		manager.finishProcessing();

		// Total stats
		PrintWriter printWriter = new PrintWriter(IOUtils.getBufferedWriter(output.getPath("header.md").toString()));
		String markdown = """
					<div class='stack-agents-table'>

					|**Total Agents**|**Stuck Agents**|**Proportion of stucket agents**|
					|:-------:|:-------:|:-------:|
					|**${totalAgents}**|**${stuckAgents}**|**${stuckAgentsProportion} %**|

					</div>

					<style>
					    .stack-agents-table {
					        font-size:20pt;
					    }

					    .stack-agents-table table {
					        width: 100%
					    }

					    .stack-agents-table table tbody tr * {
					        margin-bottom: 100px;
					    }
					</style>""";
		Map<String, String> data = new HashMap<String, String>();
		data.put("totalAgents", String.valueOf(allAgents.size()));
		data.put("stuckAgents", String.valueOf(allStuckedLinks.keySet().size()));
		data.put("stuckAgentsProportion", String.valueOf((Math.round((100.0 / allAgents.size() * allStuckedLinks.keySet().size()) * 10000))/10000.0));
		String formattedString = StrSubstitutor.replace(markdown, data);
		printWriter.println(formattedString);
		printWriter.close();

		// Per hour
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerHour.csv").toString()), CSVFormat.DEFAULT)) {

			List<String> header = new ArrayList<>(stuckAgentsPerHour.keySet());
			header.add(0, "hour");
			header.add(1, "Total");
			// Write to .csv
			printer.printRecord(header);
			for (int i = 0; i <= maxHour; i++) {
				String[] result = new String[header.size()];
				result[0] = String.valueOf(i);
				result[1] = String.valueOf(this.summarizeStuckAgentsPerHour(i));
				for (int j = 2; j < header.size(); j++) {
					result[j] = String.valueOf(stuckAgentsPerHour.get(header.get(j)).get(i));
				}
				printer.printRecord((Object[]) result);
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		// Per link
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerLink.csv").toString()), CSVFormat.DEFAULT)) {

			// Sort Map
			List<String> sorted = new ArrayList<>(allStuckedLinks.keySet());
			sorted.sort((o1, o2) -> -Double.compare(allStuckedLinks.getDouble(o1), allStuckedLinks.getDouble(o2)));
			List<String> header = new ArrayList<>(stuckAgentsPerLink.keySet());
			header.add(0, "link");
			header.add(1, "# Agents");
			// Write to .csv
			printer.printRecord(header);
			for (int i = 0; i < 20; i++) {
				String[] result = new String[header.size()];
				result[0] = sorted.get(i);
				result[1] = String.valueOf(this.summarizeStuckAgentsPerLink(sorted.get(i)));
				for (int j = 2; j < header.size(); j++) {
					result[j] = String.valueOf(stuckAgentsPerLink.get(header.get(j)).getDouble(sorted.get(i)));
				}
				printer.printRecord((Object[]) result);
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		// Stuck agents per mode
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerMode.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Mode", "# Agents");
			for (Object2IntMap.Entry<String> entry : stuckAgentsPerMode.object2IntEntrySet()) {
				printer.printRecord(entry.getKey(), entry.getIntValue());
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		// Pie chart
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerModePieChart.csv").toString()), CSVFormat.DEFAULT)) {

			List<String> header = new ArrayList<>(stuckAgentsPerMode.keySet());
			List<Integer> values = new ArrayList<>(stuckAgentsPerMode.values());

			printer.printRecord(header);
			printer.printRecord(values);
		} catch (IOException ex) {
			log.error(ex);
		}

		return 0;
	}

	private double summarizeStuckAgentsPerLink(String stuckedLink) {
		double sum = 0.;
		for (Map.Entry<String, Object2DoubleOpenHashMap<String>> entry : stuckAgentsPerLink.entrySet()) {
			sum += entry.getValue().getDouble(stuckedLink);
		}
		return sum;
	}

	private double summarizeStuckAgentsPerHour(int hour) {
		double sum = 0.;
		for (Map.Entry<String, Int2DoubleMap> entry : stuckAgentsPerHour.entrySet()) {
			sum += entry.getValue().get(hour);
		}
		return sum;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		// Pie chart
		if (!stuckAgentsPerMode.containsKey(event.getLegMode())) stuckAgentsPerMode.put(event.getLegMode(), 1);
		else stuckAgentsPerMode.put(event.getLegMode(), stuckAgentsPerMode.getInt(event.getLegMode()) + 1);

		// Stuck Agents per Hour
		Int2DoubleMap perHour = stuckAgentsPerHour.computeIfAbsent(event.getLegMode(), (k) -> new Int2DoubleOpenHashMap());
		int hour = (int) event.getTime() / 3600;
		if (hour > maxHour) maxHour = hour;
		perHour.mergeDouble(hour, 1, Double::sum);

		// Stuck Agents per Link
		Object2DoubleMap<String> perLink = stuckAgentsPerLink.computeIfAbsent(event.getLegMode(), (k) -> new Object2DoubleOpenHashMap<>());
		String link = event.getLinkId().toString();
		allStuckedLinks.merge(link, 1., Double::sum);
		perLink.mergeDouble(link, 1, Double::sum);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!allAgents.contains(event.getPersonId().toString())) allAgents.add(event.getPersonId().toString());
	}
}
