package playground.gregor.flooding;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.gregor.sims.riskaversion.RiskCostCalculator;

import com.vividsolutions.jts.geom.Envelope;

public class RiskCostFromFloodingData implements RiskCostCalculator {

	private final static Logger log = Logger
			.getLogger(RiskCostFromFloodingData.class);

	private final static double MAX_DIST = 100.;
	private final static double MAX_FLOODED_DIST = 10.;
	private final static double BASE_COST = 30 * 3600;
	private final static double BASE_TIME = 3 * 3600;

	private final NetworkLayer network;
	private final FloodingReader fr;

	private Map<Link, LinkInfo> lis;

	private FloodingLine fl;

	private NodeRiskCostsFromNetcdf nc;

	public RiskCostFromFloodingData(NetworkLayer net, FloodingReader fr) {
		this.network = net;
		this.fr = fr;
		init();
	}

	private void init() {
		// log.info("building up quad tree");
		// QuadTree<FloodingInfo> tree = buildQuad();
		// log.info("done.");
		//		
		// log.info("classifying nodes");
		// Map<Node,NodeInfo> nis = classifyNodes(tree);
		// log.info("done.");

		this.nc = new NodeRiskCostsFromNetcdf(this.network, this.fr);

		log.info("classifying nodes");
		Map<Node, NodeInfo> nis = classifyNodesII();
		log.info("done.");

		log.info("classifying links");
		this.lis = classifyLinks(nis);
		log.info("done");

//		log.info("creating shape files.");
//		new NodeCostShapeCreator(this.lis, MGC
//				.getCRS(TransformationFactory.WGS84_UTM47S));
//		log.info("done");
	}

	private Map<Node, NodeInfo> classifyNodesII() {
		Map<Node, NodeInfo> nis = new HashMap<Node, NodeInfo>();

		for (Node node : this.network.getNodes().values()) {
			FloodingInfo fi = this.nc.getNearestFloodingInfo(node);
			double dist = fi.getCoordinate().distance(
					MGC.coord2Coordinate(node.getCoord()));
			if (dist > MAX_DIST) {
				continue;
			}

			NodeInfo ni = new NodeInfo();
			ni.node = node;
			ni.time = BASE_TIME + 60 * fi.getFloodingTime();
			ni.dist = dist;
			ni.cost = this.nc.getNodeRiskCost(node);
			if (dist > MAX_FLOODED_DIST) {
				ni.cost = (BASE_COST / 2) * (1 - (dist / MAX_DIST));
			} else {
				ni.cost = (BASE_COST - 60 * fi.getFloodingTime());
			}
			nis.put(node, ni);
		}

		return nis;

	}

	private Map<Link, LinkInfo> classifyLinks(Map<Node, NodeInfo> nis) {
		Map<Link, LinkInfo> lis = new HashMap<Link, LinkInfo>();
		for (Link link : this.network.getLinks().values()) {
			NodeInfo toNode = nis.get(link.getToNode());
			if (toNode == null) {
				continue;
			}
			NodeInfo fromNode = nis.get(link.getFromNode());
			if (fromNode == null || toNode.cost > fromNode.cost) {
				LinkInfo li = new LinkInfo();
				li.link = link;
				li.time = toNode.time;
				li.cost = toNode.cost * link.getLength();
				li.baseCost = toNode.cost;
				li.dist = toNode.dist;
				lis.put(link, li);
			}

		}

		return lis;

	}

	private Map<Node, NodeInfo> classifyNodes(QuadTree<FloodingInfo> tree) {
		Map<Node, NodeInfo> nis = new HashMap<Node, NodeInfo>();

		for (Node node : this.network.getNodes().values()) {
			FloodingInfo fi = tree.get(node.getCoord().getX(), node.getCoord()
					.getY());
			double dist = fi.getCoordinate().distance(
					MGC.coord2Coordinate(node.getCoord()));
			if (dist > MAX_DIST) {
				continue;
			}
			NodeInfo ni = new NodeInfo();
			ni.node = node;
			ni.time = BASE_TIME + 60 * fi.getFloodingTime();
			ni.dist = dist;
			if (dist > MAX_FLOODED_DIST) {
				ni.cost = (BASE_COST / 2) * (1 - (dist / MAX_DIST));
			} else {
				ni.cost = (BASE_COST - 60 * fi.getFloodingTime());
			}
			nis.put(node, ni);
		}

		return nis;

	}

	private QuadTree<FloodingInfo> buildQuad() {
		Envelope e = this.fr.getEnvelope();
		QuadTree<FloodingInfo> tree = new QuadTree<FloodingInfo>(e.getMinX(), e
				.getMinY(), e.getMaxX(), e.getMaxY());
		for (FloodingInfo fi : this.fr.getFloodingInfos()) {
			tree.put(fi.getCoordinate().x, fi.getCoordinate().y, fi);
		}
		return tree;
	}

	private static class NodeInfo {
		Node node;
		double time;
		double dist;
		double cost;

	}

	static class LinkInfo {
		Link link;
		double time;
		double cost;
		double baseCost;
		double dist;
	}

	public static void main(String[] args) {
		String netcdf = "../../inputs/padang/Model_result_Houses_kst20.sww";

		FloodingReader fr = new FloodingReader(netcdf, true);

		String config = "../../inputs/configs/timeVariantEvac.xml";
		Config c = Gbl.createConfig(new String[] { config });

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());

		new RiskCostFromFloodingData(net, fr);
	}

	public double getLinkRisk(Link link, double time) {
		LinkInfo li = this.lis.get(link);
		if (li == null) {
			return 0;
		}
		return li.cost;
	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = this.lis.get(event.getLink());
		if (li == null) {
			return;
		}

		AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(), event
				.getPersonId(), li.cost / -600);
		QueueSimulation.getEvents().processEvent(e);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

}
