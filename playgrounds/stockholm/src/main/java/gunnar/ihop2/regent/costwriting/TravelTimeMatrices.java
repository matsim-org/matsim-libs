package gunnar.ihop2.regent.costwriting;

import floetteroed.utilities.Time;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
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
import org.matsim.matrices.Matrices;
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
public class TravelTimeMatrices {

	// -------------------- MEMBERS --------------------

	// TODO encapsulate
	public final List<Matrix> matrices;

	private final int startTime_s;

	private final int binSize_s;

	// -------------------- CONSTRUCTION --------------------

	public TravelTimeMatrices(
			final Network network,
			final TravelTimeCalculator ttcalc,
			final String eventsFileName,
			// final String regentMatrixFileName,
			final Set<String> relevantLinkIDs,
			final List<String> relevantZoneIDs, final ZonalSystem zonalSystem,
			final Random rnd, final int startTime_s, final int binSize_s,
			final int binCnt, final int sampleCnt) {

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;

		// run the events handling and extract the travel times

		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ttcalc);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);
		final TravelTime linkTTs = ttcalc.getLinkTravelTimes();

		// identify all zones that are relevant and contain at least one node

		final ArrayList<String> relevantAndFeasibleZoneIDs = new ArrayList<>();
		if (relevantZoneIDs == null) {
			// if no relevant zone IDs were defined then identify them self
			Logger.getLogger(MATSimDummy.class.getName()).warning(
					"no relevant zone ids given, using all zones in "
							+ "zonal system that contain at least one node");
			for (Zone zone : zonalSystem.id2zone.values()) {
				if (zonalSystem.zone2nodes.get(zone) != null
						&& zonalSystem.zone2nodes.get(zone).size() > 0) {
					relevantAndFeasibleZoneIDs.add(zone.getId());
				}
			}
		} else {
			// make sure that all relevant zones exist and contain nodes
			for (String zoneId : relevantZoneIDs) {
				final Zone zone = zonalSystem.getZone(zoneId);
				if (zone == null) {
					Logger.getLogger(MATSimDummy.class.getName()).warning(
							"zonal system does not contain zone id " + zoneId);
				} else {
					if (zonalSystem.zone2nodes.get(zone) != null
							&& zonalSystem.zone2nodes.get(zone).size() > 0) {
						relevantAndFeasibleZoneIDs.add(zoneId);
					} else {
						Logger.getLogger(MATSimDummy.class.getName()).warning(
								"zone with id " + zoneId
										+ " does not contain any nodes");
					}
				}
			}
		}

		// create the travel time matrices

		this.matrices = new ArrayList<Matrix>(binCnt);

		for (int bin = 0; bin < binCnt; bin++) {

			final Map<Tuple<Zone, Zone>, Tuple<Double, Integer>> zonePair2ttCntPair = new LinkedHashMap<>();

			final int time_s = startTime_s + bin * binSize_s + binSize_s / 2;
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Computing travel time matrix for departure time "
							+ Time.strFromSec(time_s) + ".");
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Using " + sampleCnt
							+ " randomly selected node(s) per origin zone.");
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"This can take a while.");

			// go through all origin zones
			for (String fromZoneID : relevantAndFeasibleZoneIDs) {
				final Zone fromZone = zonalSystem.id2zone.get(fromZoneID);
				// go through a sample of nodes in the origin zone
				for (Node fromNode : MathHelpers.drawWithoutReplacement(
						sampleCnt, zonalSystem.zone2nodes.get(fromZone), rnd)) {
					final LeastCostPathTree lcpt = new LeastCostPathTree(
							linkTTs, new OnlyTimeDependentTravelDisutility(
									linkTTs));
					lcpt.calculate(network, fromNode, time_s);
					// go through destination zones
					for (String toZoneID : relevantAndFeasibleZoneIDs) {
						final Zone toZone = zonalSystem.id2zone.get(toZoneID);
						for (Node toNode : zonalSystem.zone2nodes.get(toZone)) {
							final Tuple<Zone, Zone> odPair = new Tuple<>(
									fromZone, toZone);
							final double tt_min = lcpt.getTree()
									.get(toNode.getId()).getCost()
									* Units.MIN_PER_S;
							if (zonePair2ttCntPair.containsKey(odPair)) {
								final Tuple<Double, Integer> ttSumCntTupel = zonePair2ttCntPair
										.get(odPair);
								zonePair2ttCntPair.put(odPair,
										new Tuple<Double, Integer>(
												ttSumCntTupel.getA() + tt_min,
												ttSumCntTupel.getB() + 1));
							} else {
								zonePair2ttCntPair.put(odPair,
										new Tuple<Double, Integer>(tt_min, 1));
							}
						}
					}
				}
			}

			final Matrix tt = (new Matrices()).createMatrix(
					"TT_" + Time.strFromSec(time_s),
					"travel time matrix for departure time "
							+ Time.strFromSec(time_s));
			for (Map.Entry<Tuple<Zone, Zone>, Tuple<Double, Integer>> entry : zonePair2ttCntPair
					.entrySet()) {
				tt.createEntry(
						entry.getKey().getA().getId(),
						entry.getKey().getB().getId(),
						MathHelpers.round(entry.getValue().getA()
								/ entry.getValue().getB(), 2));
			}
			this.matrices.add(tt);
		}

		// ===== working original: create a single high-noon matrix =====

		// final Map<Tuple<Zone, Zone>, Tuple<Double, Integer>>
		// zonePair2ttCntPair = new LinkedHashMap<>();
		// final LeastCostPathTree lcpt = new LeastCostPathTree(linkTTs,
		// new OnlyTimeDependentTravelDisutility(linkTTs));
		// int cnt = 0;
		// final int time_s = 7 * 3600;
		//
		// for (String fromZoneID : relevantAndFeasibleZoneIDs) {
		// final Zone fromZone = zonalSystem.id2zone.get(fromZoneID);
		// Logger.getLogger(MATSimDummy.class.getName()).info(
		// "processing origin zone " + (++cnt) + " / "
		// + relevantAndFeasibleZoneIDs.size());
		//
		// final Node fromNode = MathHelpers.draw(
		// zonalSystem.zone2nodes.get(fromZone), rnd);
		// lcpt.calculate(network, fromNode, time_s);
		//
		// for (String toZoneID : relevantAndFeasibleZoneIDs) {
		// final Zone toZone = zonalSystem.id2zone.get(toZoneID);
		//
		// for (Node toNode : zonalSystem.zone2nodes.get(toZone)) {
		// final Tuple<Zone, Zone> odPair = new Tuple<>(fromZone,
		// toZone);
		// final double tt_min = lcpt.getTree().get(toNode.getId())
		// .getCost()
		// * Units.MIN_PER_S;
		// if (zonePair2ttCntPair.containsKey(odPair)) {
		// final Tuple<Double, Integer> ttSumCntTupel = zonePair2ttCntPair
		// .get(odPair);
		// zonePair2ttCntPair.put(odPair,
		// new Tuple<Double, Integer>(ttSumCntTupel.getA()
		// + tt_min, ttSumCntTupel.getB() + 1));
		// } else {
		// zonePair2ttCntPair.put(odPair,
		// new Tuple<Double, Integer>(tt_min, 1));
		// }
		// }
		// }
		// }
		//
		// // Write the result to file.
		//
		// final Matrices matrices = new Matrices();
		// final Matrix work = matrices.createMatrix("WORK",
		// "work tour travel times");
		//
		// for (Map.Entry<Tuple<Zone, Zone>, Tuple<Double, Integer>> entry :
		// zonePair2ttCntPair
		// .entrySet()) {
		// work.createEntry(
		// entry.getKey().getA().getId(),
		// entry.getKey().getB().getId(),
		// MathHelpers.round(entry.getValue().getA()
		// / entry.getValue().getB(), 2));
		// }
		//
		// final MatricesWriter writer = new MatricesWriter(matrices);
		// writer.setIndentationString("  ");
		// writer.setPrettyPrint(true);
		// writer.write(regentMatrixFileName);
	}

	public double getTT_s(final String fromZoneId, final String toZoneId,
			final int dptTime_s) {
		int bin = Math.max(0, Math.min(this.matrices.size() - 1,
				(dptTime_s - this.startTime_s) / this.binSize_s));
		return this.matrices.get(bin).getEntry(fromZoneId, toZoneId).getValue();
	}

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.matrices.size();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String networkFileName = "./data_ZZZ/run/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		int timeBinSize = 15 * 60;
		int endTime = 12 * 3600;
		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), timeBinSize, endTime, scenario
						.getConfig().travelTimeCalculator());

		final String linkAttributesFileName = "./data_ZZZ/run/link-attributes.xml";
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);
		final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));

		final String zonesShapeFileName = "./data_ZZZ/shapes/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final String eventsFileName = "./data_ZZZ/run/output/ITERS/it.0/0.events.xml.gz";
		// final String regentMatrixFileName = "./data_ZZZ/run/regent-tts.xml";

		final int startTime_s = 6 * 3600 + 1800;
		final int binSize_s = 3600;
		final int binCnt = 2;
		final int sampleCnt = 2;

		new TravelTimeMatrices(scenario.getNetwork(), ttcalc, eventsFileName,
				// regentMatrixFileName,
				relevantLinkIDs, null, zonalSystem, new Random(), startTime_s,
				binSize_s, binCnt, sampleCnt);

		System.out.println("... DONE");
	}
}

// private Set<Id<Node>> relevantNodeIDs(final Set<String> relevantLinkIDs)
// {
// final LinkedHashSet<Id<Node>> result = new LinkedHashSet<Id<Node>>();
// for (Map.Entry<Id<Link>, ? extends Link> id2link : this.network
// .getLinks().entrySet()) {
// if (relevantLinkIDs.contains(id2link.getKey().toString())) {
// result.add(id2link.getValue().getFromNode().getId());
// result.add(id2link.getValue().getToNode().getId());
// }
// }
// return result;
// }
//
// private PriorityQueue<Node> otherNodesByGeomDist(final Node node) {
// final Map<Node, Double> node2dist = new LinkedHashMap<Node, Double>();
// for (Node otherNode : this.network.getNodes().values()) {
// node2dist.put(otherNode,
// calcDistance(node.getCoord(), otherNode.getCoord()));
// }
// final PriorityQueue<Node> result = new PriorityQueue<>(11,
// new Comparator<Node>() {
// @Override
// public int compare(Node o1, Node o2) {
// return node2dist.get(o1).compareTo(node2dist.get(o2));
// }
// });
// result.addAll(this.network.getNodes().values());
// return result;
// }
//
// private Node representativeNode(final Zone zone,
// final ZonalSystem zonalSystem, final TravelTime linkTTs,
// int subListSize, final int startTime) {
//
// Node result = null;
// double minDistSum = Double.POSITIVE_INFINITY;
//
// final boolean searchAllEndNodes = true;
// final MultiNodeDijkstra router = new MultiNodeDijkstra(this.network,
// new OnlyTimeDependentTravelDisutility(linkTTs), linkTTs,
// searchAllEndNodes);
//
// System.out.println(zonalSystem.zone2nodes.get(zone).size());
//
// for (Node fromNode : zonalSystem.zone2nodes.get(zone)) {
// int cnt = 0;
// final List<InitialNode> initialNodes = new ArrayList<InitialNode>(
// subListSize);
// for (Node toNode : this.otherNodesByGeomDist(fromNode)) {
// // for (Node toNode : this.network.getNodes().values()) {
// // for (Node toNode : zonalSystem.zone2nodes.get(zone)) {
// initialNodes.add(new InitialNode(toNode, 0.0, 0.0));
// if (++cnt >= subListSize) {
// break;
// }
// }
// final Node imaginaryDestination = router
// .createImaginaryNode(initialNodes);
// router.calcLeastCostPath(fromNode, imaginaryDestination, startTime,
// null, null);
// }
//
// return result;
// }

