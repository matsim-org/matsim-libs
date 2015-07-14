package playground.staheale.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Creates a text file containing all trips based on a events file.
 * 
 * The output format is: agentId, tripStartTime, tripStartLink, tripStartLinkXCoord, tripStartLinkYCoord,
 * tripEndLink, tripEndLinkXCoord, tripEndLinkYCoord, mainMode, tripPurpose.
 * 
 * If the agent is stuck, "stuck" is assigned to the trip purpose, end link and end time are taken from the stuckAndAbort event.
 * 
 * If a pt trip only contains transit_walk legs, the main mode is transit walk.
 * 
 * @author staha
 * 
 */

public class Events2Trips {

	private static Logger log = Logger.getLogger(Events2Trips.class);

	public static void main(String[] args) throws IOException {
		String eventsFile = "C:/Users/staha/workspace/playgrounds/staheale/output/run2030combined/run.combined.150.events.xml.gz";
		String networkFile = "C:/Users/staha/workspace/playgrounds/staheale/output/multimodalNetwork2030final.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();

		ScenarioImpl  scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		log.info("Reading network xml file...");
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
		NetworkReader.readFile(networkFile);
		Network network = scenario.getNetwork();
		log.info("Reading network xml file...done.");

		TripHandler tripHandler = new TripHandler();
		events.addHandler(tripHandler);
		tripHandler.reset(0);

		log.info("Reading events file...");
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		log.info("Reading events file...done.");

		printTrips(tripHandler, network);
	}

	private static void printTrips(
			TripHandler tripHandler, Network network) throws IOException {

		final String header="agentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose";
		final BufferedWriter out =
				IOUtils.getBufferedWriter("C:/Users/staha/workspace/playgrounds/staheale/output/trips2030combined.txt");
		out.write(header);
		out.newLine();
		log.info("Writing trips file...");
		for (Id personId : tripHandler.getStartLink().getKeySet()) {
			LinkedList<Id> startLinks = tripHandler.getStartLink().get(personId);
			LinkedList<String> modes = tripHandler.getMode().get(personId);
			LinkedList<String> purposes = tripHandler.getPurpose().get(personId);
			LinkedList<Double> startTimes = tripHandler.getStartTime().get(personId);
			LinkedList<Id> endLinks = tripHandler.getEndLink().get(personId);
			LinkedList<Double> endTimes = tripHandler.getEndTime().get(personId);

			for (int i = 0; i < startLinks.size(); i++) {
				if (!personId.toString().contains("pt")) {
					if (network.getLinks().get(endLinks.get(i)) != null) {
						out.write(personId
							+ "\t"
							+startTimes.get(i)
							+ "\t"
							+startLinks.get(i)
							+ "\t"
							+ network.getLinks().get(startLinks.get(i)).getCoord().getX()
							+ "\t"
							+ network.getLinks().get(startLinks.get(i)).getCoord().getY()
							+ "\t"
							+endTimes.get(i)
							+ "\t"
							+endLinks.get(i)
							+ "\t"
							+ network.getLinks().get(endLinks.get(i)).getCoord().getX()
							+ "\t"
							+ network.getLinks().get(endLinks.get(i)).getCoord().getY()
							+ "\t"
							+modes.get(i)
							+ "\t"
							+purposes.get(i)							
							);
						out.newLine();
					}
					// in case there is no end link
					else {
						out.write(personId
								+ "\t"
								+startTimes.get(i)
								+ "\t"
								+startLinks.get(i)
								+ "\t"
								+ network.getLinks().get(startLinks.get(i)).getCoord().getX()
								+ "\t"
								+ network.getLinks().get(startLinks.get(i)).getCoord().getY()
								+ "\t"
								+endTimes.get(i)
								+ "\t"
								+endLinks.get(i)
								+ "\t"
								+ "null"
								+ "\t"
								+ "null"
								+ "\t"
								+modes.get(i)
								+ "\t"
								+purposes.get(i)							
								);
							out.newLine();
					}
				}
			}
		}
		out.flush();
		out.close();
		log.info("Writing trips file...done.");
	}

}

