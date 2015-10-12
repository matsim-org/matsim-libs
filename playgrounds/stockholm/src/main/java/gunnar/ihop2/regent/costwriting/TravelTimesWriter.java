package gunnar.ihop2.regent.costwriting;

import static org.matsim.core.utils.geometry.CoordUtils.calcDistance;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.MathHelpers;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
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
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

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

	private PriorityQueue<Node> otherNodesByGeomDist(final Node node) {
		final Map<Node, Double> node2dist = new LinkedHashMap<Node, Double>();
		for (Node otherNode : this.network.getNodes().values()) {
			node2dist.put(otherNode,
					calcDistance(node.getCoord(), otherNode.getCoord()));
		}
		final PriorityQueue<Node> result = new PriorityQueue<>(11,
				new Comparator<Node>() {
					@Override
					public int compare(Node o1, Node o2) {
						return node2dist.get(o1).compareTo(node2dist.get(o2));
					}
				});
		result.addAll(this.network.getNodes().values());
		return result;
	}

	private Node representativeNode(final Zone zone,
			final ZonalSystem zonalSystem, final TravelTime linkTTs,
			int subListSize, final int startTime) {

		Node result = null;
		double minDistSum = Double.POSITIVE_INFINITY;

		final boolean searchAllEndNodes = true;
		final MultiNodeDijkstra router = new MultiNodeDijkstra(this.network,
				new OnlyTimeDependentTravelDisutility(linkTTs), linkTTs,
				searchAllEndNodes);

		System.out.println(zonalSystem.zone2nodes.get(zone).size());

		for (Node fromNode : zonalSystem.zone2nodes.get(zone)) {
			int cnt = 0;
			final List<InitialNode> initialNodes = new ArrayList<InitialNode>(
					subListSize);
			for (Node toNode : this.otherNodesByGeomDist(fromNode)) {
				// for (Node toNode : this.network.getNodes().values()) {
				// for (Node toNode : zonalSystem.zone2nodes.get(zone)) {
				initialNodes.add(new InitialNode(toNode, 0.0, 0.0));
				if (++cnt >= subListSize) {
					break;
				}
			}
			final Node imaginaryDestination = router
					.createImaginaryNode(initialNodes);
			router.calcLeastCostPath(fromNode, imaginaryDestination, startTime,
					null, null);
		}

		return result;
	}

	public void run(final String eventsFileName,
			final String regentMatrixFileName,
			final Set<String> relevantLinkIDs, final ZonalSystem zonalSystem) {

		final Random rnd = new Random();

		// run the events handling and extract the travel times

		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this.ttcalc);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);
		final TravelTime linkTTs = this.ttcalc.getLinkTravelTimes();

		// create a single high-noon matrix

		final Map<Tuple<Zone, Zone>, Tuple<Double, Integer>> zonePair2ttCntPair = new LinkedHashMap<>();
		final LeastCostPathTree lcpt = new LeastCostPathTree(linkTTs,
				new OnlyTimeDependentTravelDisutility(linkTTs));

		int cnt = 0;
		final int time_s = 7 * 3600;
		for (Zone fromZone : zonalSystem.id2zone.values()) {
			if (zonalSystem.zone2nodes.get(fromZone) != null) {
				System.out
						.println((++cnt) + " / " + zonalSystem.id2zone.size());
				final Node fromNode = MathHelpers.draw(
						zonalSystem.zone2nodes.get(fromZone), rnd);
				lcpt.calculate(this.network, fromNode, time_s);

				for (Zone toZone : zonalSystem.id2zone.values()) {
					if (zonalSystem.zone2nodes.get(toZone) != null) {
						for (Node toNode : zonalSystem.zone2nodes.get(toZone))
						// final Node toNode = MathHelpers.draw(
						// zonalSystem.zone2nodes.get(toZone), rnd);
						{
							final Tuple<Zone, Zone> odPair = new Tuple<>(
									fromZone, toZone);
							final Tuple<Double, Integer> tt_sum_cnt = zonePair2ttCntPair
									.get(odPair);
							final double tt = lcpt.getTree()
									.get(toNode.getId()).getCost();
							zonePair2ttCntPair
									.put(odPair,
											(tt_sum_cnt == null) ? new Tuple<Double, Integer>(
													tt, 1)
													: new Tuple<Double, Integer>(
															tt_sum_cnt.getA()
																	+ tt,
															tt_sum_cnt.getB() + 1));
						}
					}
				}
			}
		}

		// Write the result to file.

		final Matrices matrices = new Matrices();
		final Matrix work = matrices.createMatrix("WORK",
				"work tour travel times");

		for (Map.Entry<Tuple<Zone, Zone>, Tuple<Double, Integer>> entry : zonePair2ttCntPair
				.entrySet()) {
			work.createEntry(
					entry.getKey().getA().getId(),
					entry.getKey().getB().getId(),
					Math.round(entry.getValue().getA()
							/ entry.getValue().getB()));
		}

		final MatricesWriter writer = new MatricesWriter(matrices);
		writer.setIndentationString("  ");
		writer.setPrettyPrint(true);
		writer.write(regentMatrixFileName);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String networkFileName = "./data/run/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		int timeBinSize = 15 * 60;
		int endTime = 12 * 3600;
		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), timeBinSize, endTime, scenario
						.getConfig().travelTimeCalculator());

		final String linkAttributesFileName = "./data/run/link-attributes.xml";
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);
		final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));

		final String zonesShapeFileName = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final TravelTimesWriter ttWriter = new TravelTimesWriter(
				scenario.getNetwork(), ttcalc);

		final String eventsFileName = "./data/run/output/ITERS/it.0/0.events.xml.gz";
		final String regentMatrixFileName = "./data/run/regent-tts.xml";
		ttWriter.run(eventsFileName, regentMatrixFileName, relevantLinkIDs,
				zonalSystem);

		System.out.println("... DONE");
	}
}
