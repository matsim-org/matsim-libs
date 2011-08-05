package playground.gregor.grips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class EvacuationNetworkGenerator {

	private static final Logger log = Logger.getLogger(EvacuationNetworkGenerator.class);

	private final Scenario sc;
	private final Geometry evacuationArea;
	private final Network network;

	private final List<Link> redundantLinks = new ArrayList<Link>();
	private final HashSet<Node> safeNodes = new HashSet<Node>();
	private final HashSet<Node> redundantNodes = new HashSet<Node>();

	private final Id safeNodeAId;

	private final Id safeNodeBId;

	private final Id safeLinkId;

	public EvacuationNetworkGenerator(Scenario sc, Geometry evavcuationArea, Id safeLinkId){
		this.sc = sc;
		this.evacuationArea = evavcuationArea;
		this.network = sc.getNetwork();
		this.safeNodeAId = this.sc.createId("en1");
		this.safeNodeBId = this.sc.createId("en2");
		this.safeLinkId = safeLinkId;
	}

	public void run() {
		log.info("generating evacuation net ...");
		log.info("classifing nodes");
		classifyNodesAndLinks();
		log.info("removing links and nodes that are outside the evacuation area");
		cleanUpNetwork();
		log.info("creating evacuation nodes and links");
		createEvacuationNodsAndLinks();
		log.info("done.");
	}

	/**
	 * Creates links from all save nodes to the evacuation node A
	 * 
	 * @param network
	 */
	private void createEvacuationNodsAndLinks() {

		Coord safeCoord = MGC.coordinate2Coord(this.evacuationArea.getCentroid().getCoordinate());

		Node safeNodeA = this.network.getFactory().createNode(this.safeNodeAId, safeCoord);
		this.network.addNode(safeNodeA);
		Node safeNodeB = this.network.getFactory().createNode(this.safeNodeBId, safeCoord);
		this.network.addNode(safeNodeB);

		double capacity = 1000000.;
		Link l = this.network.getFactory().createLink(this.safeLinkId, safeNodeA, safeNodeB);
		l.setLength(10);
		l.setFreespeed(100000);
		l.setCapacity(capacity);
		l.setNumberOfLanes(100);
		this.network.addLink(l);

		int linkId = 1;
		for (Node node : this.network.getNodes().values()) {
			Id nodeId = node.getId();
			if (this.safeNodes.contains(node) && !nodeId.equals(this.safeNodeAId) && !nodeId.equals(this.safeNodeBId)) {
				linkId++;
				String sLinkID = "el" + Integer.toString(linkId);
				Link l2 = this.network.getFactory().createLink(this.sc.createId(sLinkID), node, safeNodeA);
				l2.setLength(10);
				l2.setFreespeed(100000);
				l2.setCapacity(capacity);
				l2.setNumberOfLanes(1);
				this.network.addLink(l2);
			}
		}
	}

	private void classifyNodesAndLinks() {
		GeometryFactory geofac = new GeometryFactory();
		/*
		 * classes: 0: default, assume redundant 1: redundant node 2: save
		 * nodes, can be reached from evacuation area 3: "normal" nodes within
		 * the evacuation area
		 */
		for (Node node : this.network.getNodes().values()) {
			int inCat = 0;
			for (Link l : node.getInLinks().values()) {
				LineString ls = geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(l.getFromNode().getCoord()),MGC.coord2Coordinate(l.getToNode().getCoord())});
				if (ls.intersects(this.evacuationArea)) {
					if ((inCat == 0) || (inCat == 3)) {
						inCat = 3;
					} else {
						inCat = 2;
						break;
					}
				} else {
					this.redundantLinks.add(l);
					if (inCat <= 1) {
						inCat = 1;
					} else {
						inCat = 2;
						break;
					}
				}
			}
			switch (inCat) {
			case 2:
				this.safeNodes.add(node);
				break;
			case 3:
				break;
			case 1:
			default:
				this.redundantNodes.add(node);
			}
		}

	}
	/**
	 * Removes all links and nodes outside the evacuation area except the nodes
	 * next to the evacuation area that are reachable from inside the evacuation
	 * area ("save nodes").
	 * 
	 * @param network
	 */
	protected void cleanUpNetwork() {

		for (Link l : this.redundantLinks)
		{
			this.network.removeLink(l.getId());
		}
		log.info(this.redundantLinks.size() + " links outside the evacuation area have been removed.");
		this.redundantLinks.clear();

		for (Node n : this.redundantNodes) {
			this.network.removeNode(n.getId());
		}
		log.info(this.redundantNodes.size() + " nodes outside the evacuation area have been removed.");
		this.redundantNodes.clear();


		new NetworkCleaner().run(this.network);
	}


}
