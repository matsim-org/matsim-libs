package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import picocli.CommandLine;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes detailed information about stuck agents to an XML file.
 * This helps identify which agents failed to complete their plans, when they got stuck,
 * where they got stuck, and potentially why (based on their mode and location).
 *
 * <p>Usage: Can be run as a standalone command or integrated into event handling:</p>
 * <pre>
 * // As standalone command:
 * java -cp matsim.jar org.matsim.application.analysis.population.StuckAgentsWriter \
 *   --events events.xml.gz --output output/
 *
 * // Integrated into custom code:
 * EventsManager eventsManager = EventsUtils.createEventsManager();
 * StuckAgentsWriter writer = new StuckAgentsWriter();
 * eventsManager.addHandler(writer);
 * // ... run simulation or read events ...
 * writer.writeFile("output/stuck_agents.xml");
 * </pre>
 *
 * @author MATSim community (based on issue #3008)
 */
@CommandLine.Command(
	name = "stuck-agents-writer",
	description = "Writes detailed stuck agent information to XML file."
)
@CommandSpec(
	requireEvents = true,
	produces = {"stuck_agents.xml"}
)
public class StuckAgentsWriter extends MatsimXmlWriter implements MATSimAppCommand, PersonStuckEventHandler {

	private static final Logger log = LogManager.getLogger(StuckAgentsWriter.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(StuckAgentsWriter.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(StuckAgentsWriter.class);

	/**
	 * Stores information about a single stuck agent event.
	 */
	private static class StuckAgentRecord {
		final String personId;
		final double time;
		final String linkId;
		final String legMode;
		final String reason;

		StuckAgentRecord(String personId, double time, String linkId, String legMode, String reason) {
			this.personId = personId;
			this.time = time;
			this.linkId = linkId;
			this.legMode = legMode;
			this.reason = reason;
		}
	}

	private final List<StuckAgentRecord> stuckAgents = new ArrayList<>();

	public static void main(String[] args) {
		new StuckAgentsWriter().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		log.info("Reading events and collecting stuck agent information...");

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);
		manager.initProcessing();

		EventsUtils.readEvents(manager, input.getEventsPath());

		manager.finishProcessing();

		String outputPath = output.getPath("stuck_agents.xml").toString();
		writeFile(outputPath);

		log.info("Wrote {} stuck agent records to {}", stuckAgents.size(), outputPath);

		return 0;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		String personId = event.getPersonId().toString();
		double time = event.getTime();
		Id<Link> linkId = event.getLinkId();
		String linkIdStr = linkId != null ? linkId.toString() : "unknown";
		String legMode = event.getLegMode() != null ? event.getLegMode() : "unknown";
		String reason = event.getReason() != null ? event.getReason() : "";

		stuckAgents.add(new StuckAgentRecord(personId, time, linkIdStr, legMode, reason));

		if (log.isDebugEnabled()) {
			log.debug("Agent stuck: personId={}, time={}, linkId={}, mode={}, reason={}",
				personId, Time.writeTime(time), linkIdStr, legMode, reason);
		}
	}

	/**
	 * Writes the collected stuck agent information to an XML file.
	 *
	 * @param filename the path to the output XML file
	 * @throws UncheckedIOException if writing fails
	 */
	public void writeFile(String filename) throws UncheckedIOException {
		log.info("Writing {} stuck agents to {}", stuckAgents.size(), filename);

		openFile(filename);
		writeXmlHead();
		writeDoctype("stuckAgents", "http://www.matsim.org/files/dtd/stuckAgents_v1.dtd");
		writeStartTag("stuckAgents", List.of(
			createTuple("count", stuckAgents.size())
		));

		for (StuckAgentRecord record : stuckAgents) {
			List<org.matsim.core.utils.collections.Tuple<String, String>> attributes = new ArrayList<>();
			attributes.add(createTuple("personId", record.personId));
			attributes.add(createTuple("time", Time.writeTime(record.time)));
			attributes.add(createTuple("linkId", record.linkId));
			attributes.add(createTuple("legMode", record.legMode));
			if (record.reason != null && !record.reason.isEmpty()) {
				attributes.add(createTuple("reason", record.reason));
			}

			writeStartTag("stuckAgent", attributes, true);
		}

		writeEndTag("stuckAgents");
		close();

		log.info("Successfully wrote stuck agents file");
	}

	/**
	 * Resets the collected stuck agent records.
	 * Useful if you want to reuse the same writer instance.
	 */
	public void reset() {
		stuckAgents.clear();
	}

	/**
	 * Returns the number of stuck agents recorded so far.
	 */
	public int getStuckAgentCount() {
		return stuckAgents.size();
	}
}
