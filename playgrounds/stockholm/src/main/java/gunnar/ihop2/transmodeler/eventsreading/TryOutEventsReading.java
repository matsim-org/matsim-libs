package gunnar.ihop2.transmodeler.eventsreading;

import java.util.ArrayList;
import java.util.List;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.Time;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TryOutEventsReading implements EventHandler {

	TryOutEventsReading() {
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "./data/transmodeler/";
		final String matsimNetworkFile = path + "network.xml";
		final String matsimEventsFile = path + "event_stockholm_150703.xml";

		System.out.println("creating config");
		final Config config = ConfigUtils.createConfig();

		System.out.println("creating scenario");
		final Scenario scenario = ScenarioUtils.createScenario(config);

		System.out.println("reading network");
		final NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(
				scenario.getNetwork());
		networkReader.readFile(matsimNetworkFile);

		// defining links of interest and checking if they are there
		final List<Id<Link>> observedLinks = new ArrayList<Id<Link>>();
		observedLinks.add(Id.createLinkId("33457_SE"));
		observedLinks.add(Id.createLinkId("74952_NW"));
		observedLinks.add(Id.createLinkId("28318_NW"));
		observedLinks.add(Id.createLinkId("97634_S"));
		observedLinks.add(Id.createLinkId("1253_NW"));
		observedLinks.add(Id.createLinkId("62105_S"));
		observedLinks.add(Id.createLinkId("1253_NW"));
		observedLinks.add(Id.createLinkId("34748_N"));

		for (Id<Link> linkId : observedLinks) {
			if (!scenario.getNetwork().getLinks().containsKey(linkId)) {
				throw new RuntimeException("link " + linkId + " not in network");
			}
		}

		// we want data from 6:00 to 8:30 in bins of 5 min size
		final int timeBinSize = 5 * 60;
		final int endTime = 9 * 3600;
		final VolumesAnalyzer volcalc = new VolumesAnalyzer(timeBinSize,
				endTime, scenario.getNetwork());

		final LinkLeaveCounter leaveCounter = new LinkLeaveCounter();

		System.out.println("reading events");
		final TryOutEventsReading handler = new TryOutEventsReading();
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		events.addHandler(volcalc);
		events.addHandler(leaveCounter);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(matsimEventsFile);

		// now write out the data of interest
		for (Id<Link> linkId : observedLinks) {
			System.out
					.println(linkId + ":\t" + leaveCounter.id2cnt.get(linkId));
		}
		System.out.println("total (entire network):\t"
				+ leaveCounter.totalLeaves);

		// for (Map.Entry<Id<Link>, Long> link2cntEntry : leaveCounter.id2cnt
		// .entrySet()) {
		// System.out.println(link2cntEntry);
		// }
		// System.out.println("number of distinct links:\t"
		// + leaveCounter.id2cnt.size() + " out of "
		// + scenario.getNetwork().getLinks().size());

		System.out.print("time");
		for (Id<Link> linkId : observedLinks) {
			System.out.print("\t" + linkId);
		}
		System.out.println();
		for (int bin = 0; bin < volcalc.getVolumesArraySize(); bin++) {
			System.out.print(Time.strFromSec(bin * timeBinSize));
			for (Id<Link> linkId : observedLinks) {
				System.out.print("\t");
				final int[] vols = volcalc.getVolumesForLink(linkId);
				if (vols == null) {
					System.out.print(0);
				} else {
					System.out.print(vols[bin]);
				}
			}
			System.out.println();
		}

		System.out.println("... DONE");

	}

	@Override
	public void reset(int iteration) {
	}
}
