package org.matsim.simwrapper.analysis;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "todo", description = "todo")
@CommandSpec(requireEvents = true, produces = {"stuckAgentsPerHour.csv", "piechart.csv"})
public class StuckAgentAnalysis implements MATSimAppCommand, PersonStuckEventHandler {

	private final Object2IntMap<String> pieChart = new Object2IntOpenHashMap<>();
	private final Map<String, Int2DoubleMap> stuckAgentsPerHour = new HashMap<>();
	private final Map<String, Object2DoubleMap<String>> stuckAgentsPerLink = new HashMap<>();
	private int maxHour = 0;

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(StuckAgentAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(StuckAgentAnalysis.class);

	public static void main(String[] args) {
		new StuckAgentAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);

		manager.initProcessing();

		EventsUtils.readEvents(manager, input.getEventsPath());

		System.out.println(stuckAgentsPerLink);

		// Per hour
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerHour.csv").toString()), CSVFormat.DEFAULT)) {

			List<String> header = new ArrayList<>(stuckAgentsPerHour.keySet());
			header.add(0, "hour");
			// Write to .csv
			printer.printRecord(header);
			for ( int i = 0; i <= maxHour; i ++) {
				String[] result = new String[header.size()];
				result[0] = String.valueOf(i);
				for (int j = 1; j < header.size(); j++) {
					result[j] = String.valueOf(stuckAgentsPerHour.get(header.get(j)).get(i));
				}
				printer.printRecord((Object[]) result);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// Per link
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuckAgentsPerHour.csv").toString()), CSVFormat.DEFAULT)) {

			System.out.println(stuckAgentsPerLink);

			List<String> header = new ArrayList<>(stuckAgentsPerLink.keySet());
			header.add(0, "link");
			// Write to .csv
			printer.printRecord(header);
			for ( int i = 0; i <= maxHour; i ++) {
				String[] result = new String[header.size()];
				result[0] = String.valueOf(i);
				for (int j = 1; j < header.size(); j++) {
					result[j] = String.valueOf(stuckAgentsPerLink.get(header.get(j)).get(i));
				}
				printer.printRecord((Object[]) result);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// Pie chart
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("piechart.csv").toString()), CSVFormat.DEFAULT)) {

			List<String> header = new ArrayList<>(pieChart.keySet());
			List<Integer> values = new ArrayList<>(pieChart.values());

			printer.printRecord(header);
			printer.printRecord(values);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		manager.finishProcessing();

		return 0;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (event.getEventType().equals("stuckAndAbort")) {
			// Pie chart
			if (!pieChart.containsKey(event.getLegMode())) pieChart.put(event.getLegMode(), 1);
			else pieChart.put(event.getLegMode(), pieChart.get(event.getLegMode()) + 1);

			// Stuck Agents per Hour
			Int2DoubleMap perHour = stuckAgentsPerHour.computeIfAbsent(event.getLegMode(), (k) -> new Int2DoubleOpenHashMap());
			int hour = (int) event.getTime() / 3600;
			if (hour > maxHour) maxHour = hour;
			perHour.mergeDouble(hour, 1, Double::sum);

			// Stuck Agents per Link
			Object2DoubleMap<String> perLink = stuckAgentsPerLink.computeIfAbsent(event.getLegMode(), (k) -> new Object2DoubleOpenHashMap<>());
			String link = event.getLinkId().toString();
			perLink.mergeDouble(link, 1, Double::sum);


//			// Stuck Agents per Hour
//			String link = event.getLinkId().toString();
//			if (!stuckAgentsPerLink.containsKey(link)) stuckAgentsPerLink.put(link, new Object2DoubleOpenHashMap<>());
//
//			if(stuckAgentsPerLink.get(link).containsKey(event.getLegMode())) {
//				stuckAgentsPerLink.get(link).put(event.getLegMode(), stuckAgentsPerLink.get(link).getDouble(event.getLegMode()) + 1);
//			} else {
//				stuckAgentsPerLink.get(link).put(event.getLegMode(), 1.);
//			}

		}
	}
}
