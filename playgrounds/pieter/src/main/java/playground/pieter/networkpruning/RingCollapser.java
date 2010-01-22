package playground.pieter.networkpruning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkSummary;
import org.matsim.core.utils.geometry.CoordImpl;

public class RingCollapser {
	private NetworkLayer network;
	private final double maxLength = Math.PI*1000; //circumference of a circle of 1km diameter
	private final double streetCap = 999; //this ensures only streets get removed, not major routes
	private final double capPeriod;
	private String inFile;
	private String outFile;
	private final int numberOfIterations = 2;
	private int ringCollapseCount;
	private int maxNodeId;

	public RingCollapser(String inFile, String outFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.inFile = inFile;
		this.outFile = outFile;
		new MatsimNetworkReader(scenario).readFile(this.inFile);
		new NetworkSummary().run(network);
		this.capPeriod = network.getCapacityPeriod();
	}
	
	//constructor when class gets called to go to work on existing networklayer
	public RingCollapser(NetworkLayer network){
		this.network = network;
		this.capPeriod = network.getCapacityPeriod();
	}

	public void run(){
		for (int i=0; i<this.numberOfIterations; i++){
			System.out.println("Iteration number " + i);
			removeRings();
//			new NetworkCleaner().run(network);
		}
//		new NetworkCalcTopoType().run(network);
//		new NetworkSummary().run(network);
		
		System.out.println("Number of rings collapsed: " + this.ringCollapseCount);
		
		if(!this.outFile.equals(null)){
			new NetworkWriter(this.network).writeFile(this.outFile);			
		}
		System.out.println("File written to "+ this.outFile);
	}

	private void removeRings() {
		findMaxNodeId();
//		Map<Id, Node> nodeMap = this.network.getNodes();
		ArrayList<NodeImpl> originalNodes = new ArrayList<NodeImpl>();
		originalNodes.addAll(this.network.getNodes().values());
		Iterator<NodeImpl> nodeIterator = originalNodes.iterator();
		while(nodeIterator.hasNext()){
			NodeImpl currentNode = nodeIterator.next();
			if(!checkIfNodeQualifies(currentNode))
				continue;
			LoopSeekerRoot rootNode = new LoopSeekerRoot(currentNode, this.maxLength, this.streetCap, this.capPeriod);
			if(!rootNode.loopFound)	// no ring was found, move on to next node
				continue;
//			a ring was found, all links to and from ringnodes (except for the links making up the ring) need to be connected to the centroid;
			Node centroidNode = createCentroidNode(rootNode.ringNodes);
			rootNode.weldLinks(centroidNode);
			break;
		}
		
		
	}

	private void findMaxNodeId() {
		int max_node_id = Integer.MIN_VALUE;
		for (NodeImpl node : network.getNodes().values()) {
			int node_getID = Integer.parseInt(node.getId().toString());
			if (max_node_id < node_getID) { max_node_id = node_getID; }
		}
		this.maxNodeId = max_node_id;
		
	}

	private boolean checkIfNodeQualifies(NodeImpl currentNode) {
		boolean isStreetNode = false;
		boolean isThruNode = false;
//		first check that it has at least two inLinks and 2 outLinks, and that they are of street capacity
		Map<Id, ? extends Link> inLinkMap = currentNode.getInLinks();
		Map<Id, ? extends Link> outLinkMap = currentNode.getOutLinks();
		if(inLinkMap.size() >= 2 && outLinkMap.size() >= 2)
			isThruNode = true;
		isStreetNode = checkIfStreetNode(inLinkMap) && checkIfStreetNode(outLinkMap);
		if(isThruNode && isStreetNode)
			return true;
		return false;
		
	}



	private boolean checkIfStreetNode(Map<Id, ? extends Link> linkMap) {
		Iterator<? extends Link> linkIterator = linkMap.values().iterator();
		while(linkIterator.hasNext()){
			Link currentLink = linkIterator.next();
			if(currentLink.getCapacity(this.capPeriod) != this.streetCap)
				return false;
		}
		return true;
	}


	private Node createCentroidNode(ArrayList<Node> ringNodePath) {
			double averageX = 0;
			double averageY = 0;
			int nodeCount = 0;
			Iterator<Node> nodeIterator = ringNodePath.iterator();
			while (nodeIterator.hasNext()){
				Coord currentCoord = nodeIterator.next().getCoord();
				averageX += currentCoord.getX();
				averageY += currentCoord.getY();
				nodeCount++;
			}
			averageX /= nodeCount;
			averageY /= nodeCount;
	//		create centroidNode;
			Node centroidNode = this.network.createAndAddNode(new IdImpl(++this.maxNodeId), new CoordImpl(averageX, averageY));
			return centroidNode;
		}

	public static void main(String args[]){
		String inFile = "./southafrica/network/sample_network_1000m+.xml";
		String outFile = "./southafrica/network/sample_network_1000m+_norings.xml";
		new RingCollapser(inFile,outFile).run();

	}
}
