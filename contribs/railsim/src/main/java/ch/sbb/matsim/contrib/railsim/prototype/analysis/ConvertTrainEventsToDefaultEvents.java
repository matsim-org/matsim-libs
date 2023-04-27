package ch.sbb.matsim.contrib.railsim.prototype.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author Ihab Kaddoura
 */
public class ConvertTrainEventsToDefaultEvents {
	private static final Logger log = LogManager.getLogger(ConvertTrainEventsToDefaultEvents.class);

	public static void main(String[] args) {

		String outputDirectory = "test/output/ch/sbb/railsim/RunRailsimTest/test4/";
		String runId = "test";

		ConvertTrainEventsToDefaultEvents analysis = new ConvertTrainEventsToDefaultEvents();
		analysis.run(runId, outputDirectory);
	}

	public void run(String runId, String outputDirectory) {

		if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory.concat("/");

		String eventsFile = outputDirectory + runId + ".output_events.xml.gz";

		// read events
		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML eventWriter = new EventWriterXML(outputDirectory + runId + ".output_events_trainVisualization.xml.gz");

		TrainEventsHandler trainEventHandler = new TrainEventsHandler(events);
		events.addHandler(eventWriter);
		events.addHandler(trainEventHandler);
		events.initProcessing();

		log.info("Reading the events...");
		new TrainEventsReader(events).readFile(eventsFile);
		events.finishProcessing();
		log.info("Reading the events... Done.");

		eventWriter.closeFile();
	}

}
