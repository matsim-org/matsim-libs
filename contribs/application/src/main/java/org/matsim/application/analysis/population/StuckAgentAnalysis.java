package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@CommandLine.Command(name = "stuck-agents", description = "Generates statistics for stuck agents.")
@CommandSpec(requireEvents = true, produces = {"stuck_agents_per_hour.csv", "stuck_agents_per_mode.csv", "stuck_agents_per_link.csv", "stuck_agents.csv"})
public class StuckAgentAnalysis implements MATSimAppCommand, PersonStuckEventHandler, ActivityStartEventHandler {
	private static final Logger log = LogManager.getLogger(StuckAgentAnalysis.class);
	private final Object2IntMap<String> stuckAgentsPerMode = new Object2IntOpenHashMap<>();

	/**
	 * Per mode, per hour -> number of stuck agents
	 */
	private final Map<String, Int2DoubleMap> stuckAgentsPerHour = new HashMap<>();
	private final Map<String, Object2DoubleOpenHashMap<String>> stuckAgentsPerLink = new HashMap<>();
	private final Object2DoubleOpenHashMap<String> allStuckLinks = new Object2DoubleOpenHashMap<>();
	private final Set<String> allAgents = new HashSet<>();
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
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuck_agents.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Total mobile Agents", allAgents.size(), "user-group");
			printer.printRecord("Stuck Agents", allStuckLinks.keySet().size(), "person-circle-xmark");
			printer.printRecord("Proportion of stuck agents", new DecimalFormat("#.0#", DecimalFormatSymbols.getInstance(Locale.US)).format(((Math.round((100.0 / allAgents.size() * allStuckLinks.keySet().size()) * 100)) / 100.0)) + '%', "chart-pie");
		} catch (IOException ex) {
			log.error(ex);
		}

		// Per hour
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuck_agents_per_hour.csv").toString()), CSVFormat.DEFAULT)) {
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
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuck_agents_per_link.csv").toString()), CSVFormat.DEFAULT)) {

			// Sort Map
			List<String> sorted = new ArrayList<>(allStuckLinks.keySet());
			sorted.sort((o1, o2) -> -Double.compare(allStuckLinks.getDouble(o1), allStuckLinks.getDouble(o2)));

			List<String> header = new ArrayList<>(stuckAgentsPerLink.keySet());
			header.add(0, "link");
			header.add(1, "Agents");
			// Write to .csv
			printer.printRecord(header);
			for (int i = 0; i < 20 && i < sorted.size(); i++) {
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
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("stuck_agents_per_mode.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Mode", "Agents");
			for (Object2IntMap.Entry<String> entry : stuckAgentsPerMode.object2IntEntrySet()) {
				printer.printRecord(entry.getKey(), entry.getIntValue());
			}
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
		stuckAgentsPerMode.mergeInt(event.getLegMode(), 1, Integer::sum);

		// Stuck Agents per Hour
		Int2DoubleMap perHour = stuckAgentsPerHour.computeIfAbsent(event.getLegMode(), (k) -> new Int2DoubleOpenHashMap());
		int hour = (int) event.getTime() / 3600;
		if (hour > maxHour) maxHour = hour;
		perHour.mergeDouble(hour, 1, Double::sum);

		// Stuck Agents per Link
		Object2DoubleMap<String> perLink = stuckAgentsPerLink.computeIfAbsent(event.getLegMode(), (k) -> new Object2DoubleOpenHashMap<>());

		Id<Link> link = Objects.requireNonNullElseGet(event.getLinkId(), () -> Id.createLinkId("unknown"));
		allStuckLinks.mergeDouble(link.toString(), 1., Double::sum);
		perLink.mergeDouble(link.toString(), 1, Double::sum);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		allAgents.add(event.getPersonId().toString());
	}
}
