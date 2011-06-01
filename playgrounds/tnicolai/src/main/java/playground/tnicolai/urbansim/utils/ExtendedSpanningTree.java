package playground.tnicolai.urbansim.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

public class ExtendedSpanningTree{
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(ExtendedSpanningTree.class);
	
	private Node origin = null;
	private double dTime = Time.UNDEFINED_TIME;

	private final TravelTime ttFunction;
	private final TravelCost tcFunction;
	private HashMap<Id,NodeData> nodeData;
	
	public final int isTravelTime = 0;
	public final int isTravelCost = 1;
	public final int isTravelDistance = 2;
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public ExtendedSpanningTree(TravelTime tt, TravelCost tc){
		log.info("init " + this.getClass().getName() + " module...");
		this.ttFunction = tt;
		this.tcFunction = tc;
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////
	
	public static class NodeData {
		private Node prev = null;
		private double cost = Double.MAX_VALUE;
		private double time = Double.MAX_VALUE;
		private double distance = Double.MAX_VALUE;
		public void reset() { this.prev = null; this.cost = Double.MAX_VALUE; this.time = Double.MAX_VALUE; this.distance = Double.MAX_VALUE; }
		public void visit(final Node comingFrom, final double cost, final double time, final double distance) {
			this.prev = comingFrom;
			this.cost = cost;
			this.time = time;
			this.distance = distance;
		}
		public double getCost() { return this.cost; }
		public double getTime() { return this.time; }
		public double getDistance() { return this.distance; }
		public Node getPrevNode() { return this.prev; }
	}
	
	static class ComparatorCost implements Comparator<Node> {
		protected Map<Id, ? extends NodeData> nodeData;
		ComparatorCost(final Map<Id, ? extends NodeData> nodeData) { this.nodeData = nodeData; }
		public int compare(final Node n1, final Node n2) {
			double c1 = getCost(n1);
			double c2 = getCost(n2);
			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return n1.getId().compareTo(n2.getId());
		}
		protected double getCost(final Node node) { return this.nodeData.get(node.getId()).getCost(); }
	}
	
	static class ComparatorTime implements Comparator<Node> {
		protected Map<Id, ? extends NodeData> nodeData;
		ComparatorTime(final Map<Id, ? extends NodeData> nodeData) { this.nodeData = nodeData; }
		public int compare(final Node n1, final Node n2) {
			double c1 = getTime(n1);
			double c2 = getTime(n2);
			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return n1.getId().compareTo(n2.getId());
		}
		protected double getTime(final Node node) { return this.nodeData.get(node.getId()).getTime(); }
	}
	
	static class ComparatorDistance implements Comparator<Node> {
		protected Map<Id, ? extends NodeData> nodeData;
		ComparatorDistance(final Map<Id, ? extends NodeData> nodeData) { this.nodeData = nodeData; }
		public int compare(final Node n1, final Node n2) {
			double c1 = getDistance(n1);
			double c2 = getDistance(n2);
			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return n1.getId().compareTo(n2.getId());
		}
		protected double getDistance(final Node node) { return this.nodeData.get(node.getId()).getDistance(); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigin(Node origin) {
		this.origin = origin;
	}

	public final void setDepartureTime(double time) {
		this.dTime = time;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final HashMap<Id,NodeData> getTree() {
		return this.nodeData;
	}

	public final TravelTime getTravelTimeCalculator() {
		return this.ttFunction;
	}

	public final TravelCost getTravelCostCalulator() {
		return this.tcFunction;
	}

	public final Node getOrigin() {
		return this.origin;
	}

	public final double getDepartureTime() {
		return this.dTime;
	}
		
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void relaxNode(final Node n, PriorityQueue<Node> pendingNodes, int comparatorType ) {
		NodeData nData = nodeData.get(n.getId());
		double currTime = nData.getTime();
		double currCost = nData.getCost();
		double currDist = nData.getDistance();
		for (Link l : n.getOutLinks().values()) {
			Node nn = l.getToNode();
			NodeData nnData = nodeData.get(nn.getId());
			if (nnData == null) { nnData = new NodeData(); this.nodeData.put(nn.getId(),nnData); }
			double visitCost = currCost+tcFunction.getLinkGeneralizedTravelCost(l,currTime);
			double visitTime = currTime+ttFunction.getLinkTravelTime(l,currTime);
			double visitDistance = currDist+l.getLength();
			
			if (comparatorType == isTravelCost && visitCost < nnData.getCost()) {
				pendingNodes.remove(nn);
				nnData.visit(n,visitCost,visitTime, visitDistance);
				pendingNodes.add(nn);
			}
			else if(comparatorType == isTravelTime && visitTime < nnData.getTime()) {
				pendingNodes.remove(nn);
				nnData.visit(n,visitCost,visitTime, visitDistance);
				pendingNodes.add(nn);
			}
			else if(comparatorType == isTravelDistance && visitDistance < nnData.getDistance()) {
				pendingNodes.remove(nn);
				nnData.visit(n,visitCost,visitTime, visitDistance);
				pendingNodes.add(nn);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final Network network, final int comparatorType) {

		nodeData = new HashMap<Id,NodeData>((int)(network.getNodes().size()*1.1),0.95f);
		NodeData d = new NodeData();
		d.time = dTime;
		d.cost = 0.;
		d.distance = 0.;
		nodeData.put(origin.getId(),d);
		
		PriorityQueue<Node> pendingNodes = null;
		
		if(comparatorType == isTravelCost){
			ComparatorCost comparator = new ComparatorCost(nodeData);
			pendingNodes = new PriorityQueue<Node>(500,comparator);
		}
		else if (comparatorType == isTravelDistance){
			ComparatorDistance comparator = new ComparatorDistance(nodeData);
			pendingNodes = new PriorityQueue<Node>(500,comparator);
		}
		else if (comparatorType == isTravelTime){
			ComparatorTime comparator = new ComparatorTime(nodeData);
			pendingNodes = new PriorityQueue<Node>(500,comparator);
		}
		relaxNode(this.origin,pendingNodes, comparatorType);
		while (!pendingNodes.isEmpty()) {
			Node n = pendingNodes.poll();
			relaxNode(n,pendingNodes, comparatorType);
		}
	}
	
}
