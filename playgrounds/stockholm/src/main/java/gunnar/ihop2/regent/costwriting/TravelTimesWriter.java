package gunnar.ihop2.regent.costwriting;

import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TravelTimesWriter {

	// -------------------- MEMBERS --------------------

	private final Network network;

	private final TravelTimeCalculator ttcalc;

	// -------------------- CONSTRUCTION --------------------

	public TravelTimesWriter(final Network network,
			final TravelTimeCalculator ttcalc) {
		this.network = network;
		this.ttcalc = ttcalc;
	}

	// -------------------- IMPLEMENTATION --------------------

	private Set<Id<Node>> relevantNodeIDs(final Set<String> relevantLinkIDs) {

		/*
		 * TODO CONTINUE HERE (AND THINK FIRST!)
		 * 
		 * The travel times computed here need to be representative and
		 * identifiable at the zone-to-zone level. Revisit need to maintain
		 * consistency with Transmodeler's network representation!
		 */

		final LinkedHashSet<Id<Node>> result = new LinkedHashSet<Id<Node>>();
		for (Map.Entry<Id<Link>, ? extends Link> id2link : this.network
				.getLinks().entrySet()) {
			if (relevantLinkIDs.contains(id2link.getKey().toString())) {
				result.add(id2link.getValue().getFromNode().getId());
				result.add(id2link.getValue().getToNode().getId());
			}
		}
		return result;
	}

	public void run(final String eventsFileName,
			final String regentMatrixFileName, final Set<String> relevantLinkIDs) {

		// run the event handling

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this.ttcalc);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);

		// now try to get something out of this ... some travel times ...

		final TravelTime linkTTs = this.ttcalc.getLinkTravelTimes();

		for (Map.Entry<Id<Link>, ? extends Link> id2link : this.network
				.getLinks().entrySet()) {
			System.out
					.println(id2link.getKey()
							+ "\t"
							+ (linkTTs.getLinkTravelTime(id2link.getValue(),
									8 * 3600, null, null) / (id2link.getValue()
									.getLength() / id2link.getValue()
									.getFreespeed())));
		}

		// ... and some routing!

		LeastCostPathTree lcpt = new LeastCostPathTree(linkTTs,
				new OnlyTimeDependentTravelDisutility(linkTTs));

		for (Id<Node> nodeId : this.relevantNodeIDs(relevantLinkIDs)) {
			final Node node = this.network.getNodes().get(nodeId);
			System.out.println("computing a shortest path tree for node "
					+ node.getId());
			lcpt.calculate(this.network, node, 8 * 3600);
		}

		// for (Node node : this.network.getNodes().values()) {
		// System.out.println("computing a shortest path tree for node "
		// + node.getId());
		// lcpt.calculate(this.network, node, 8 * 3600);
		// }
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String networkFileName = "./data/saleem/network.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		int timeBinSize = 15 * 60;
		int endTime = 12 * 3600;
		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), timeBinSize, endTime, scenario
						.getConfig().travelTimeCalculator());

		final String linkAttributesFileName = "./data/saleem/linkAttributes.xml";
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);
		final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));

		final TravelTimesWriter ttWriter = new TravelTimesWriter(
				scenario.getNetwork(), ttcalc);

		final String eventsFileName = "./data/saleem/output/ITERS/it.0/0.events.xml.gz";
		final String regentMatrixFileName = null; // TODO
		ttWriter.run(eventsFileName, regentMatrixFileName, relevantLinkIDs);

		System.out.println("... DONE");
	}
}
