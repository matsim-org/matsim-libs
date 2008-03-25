/* *********************************************************************** *
 * project: org.matsim.*
 * DCLogit.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.multipath;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;

import playground.gregor.vis.LinkPainter;

/**
 * This is the implementation of the D-C-Logit algorithm introduced by Russo, F. & Vitetta (A. An assignment model with modified Logit, which obviates enumeration and overlapping problems Transportation, 2003, 30, 177-201)
 * and is based on the Dial algorithm (Dial, R. B. A probabilistic traffic assignment model which obviates path enumeration Transportation Research, 1971, 5, 83-111)
 * with an extension to take the path overlap into account too (Cascetta ... )
 *
 *
 * @author glaemmel
 *
 */
@Deprecated
public class DCLogit implements LeastCostPathCalculator{

	private final static Logger log = Logger.getLogger(DCLogit.class);
	/**
	 * The network on which we find routes.
	 */
	final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelCostI costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTimeI timeFunction;


	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */
	protected ComparatorDCLogitCost comparator;


	final private HashMap<IdI, DCLogitNodeData> nodeData;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;

	boolean doGatherInformation = true;

	double avgRouteLength = 0;

	double avgTravelTime = 0;

	int routeCnt = 0;

	int revisitNodeCount = 0;

	int visitNodeCount = 0;

	private BeelineDifferenceTracer tracer;

	//TODO for debugging only
	protected DisplayNetStateWriter netStateWriter = null;
	private int time;

	public DCLogit(NetworkLayer network, TravelCostI costFunction, TravelTimeI timeFunction){

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<IdI, DCLogitNodeData>((int)(network.getNodes().size() * 1.1), 0.95f);
		this.comparator = new ComparatorDCLogitCost(this.nodeData);

		//TODO DEBUG
		initSnapShotWriter();
		this.time = 0;

	}

	//TODO debug
	private void initSnapShotWriter() {

		String snapshotFile = "./output/Snapshot";

		Config config = Gbl.getConfig();
		// OR do it like this: buffers = Integer.parseInt(Config.getSingleton().getParam("temporal", "buffersize"));
		// Automatic reasoning about buffersize, so that the file will be about 5MB big...
		int buffers = this.network.getLinks().size();
		String buffString = config.findParam("vis", "buffersize");
		if (buffString == null) {
			buffers = Math.max(5, Math.min(50000/buffers, 100));
		} else buffers = Integer.parseInt(buffString);

		this.netStateWriter = new LinkPainter(this.network, config.network().getInputFile(), snapshotFile, 1, buffers);
		this.netStateWriter.open();

	}

	public Route calcLeastCostPath(Node fromNode, Node toNode, double startTime) {

		PriorityQueue<Node> pendingNodes = new PriorityQueue<Node>(500, this.comparator);

		double arrivalTime = 0;

		this.tracer = new BeelineDifferenceTracer(fromNode.getCoord(), toNode.getCoord());


		// The forward path - spans the Dijkstra like shortest path tree

		boolean stillSearching = true;

		augmentIterationID();

		initFromNode(fromNode, toNode, startTime, pendingNodes);

		int snapShotSlowDown = 50;
		int count = 0;

		while (stillSearching) {

			//TODO DEBUG
			if (count++ >= snapShotSlowDown) {
				try {
					count = 0;
					this.netStateWriter.dump(this.time++);
					if (this.time > 240){
			            try {
			                this.netStateWriter.close();
			            } catch (IOException e) {
			                e.printStackTrace();
			            }
			            this.netStateWriter = null;

					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}



			Node outNode = pendingNodes.poll();
			if (!getData(outNode).hasOpenLinks())
				continue;
			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId()
								+ " to node " + toNode.getId());
				return null;
			}


			if (outNode.getId() == toNode.getId()) {
				stillSearching = false;
				DCLogitNodeData outData = getData(outNode);
				arrivalTime = outData.getTime();
			} else {
				relaxNode(outNode, toNode, pendingNodes);
			}
		}




		// now construct the route
		ArrayList<Node> routeNodes = new ArrayList<Node>();
		Node tmpNode = toNode;
		while (tmpNode.getId() != fromNode.getId()) {
			routeNodes.add(0, tmpNode);
			DCLogitNodeData tmpData = getData(tmpNode);
			tmpNode = tmpData.drawNode();

		}
		routeNodes.add(0, tmpNode); // add the fromNode at the beginning of the list

		DCLogitNodeData toNodeData = getData(toNode);
		Route route = new Route();
		route.setRoute(routeNodes, (int) (arrivalTime - startTime), toNodeData.cost);

		if (this.doGatherInformation) {
			this.avgTravelTime = (this.routeCnt * this.avgTravelTime + route
					.getTravTime()) / (this.routeCnt + 1);
			this.avgRouteLength = (this.routeCnt * this.avgRouteLength + route
					.getDist()) / (this.routeCnt + 1);
			this.routeCnt++;
		}


		//TODO DEBUG
		((LinkPainter)this.netStateWriter).reset();
		colorizeEfficientPaths(toNode,fromNode);
			try {
				this.netStateWriter.dump(this.time++);
				this.netStateWriter.dump(this.time++);
				this.netStateWriter.dump(this.time++);this.netStateWriter.dump(this.time++);this.netStateWriter.dump(this.time++);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
                this.netStateWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.netStateWriter = null;




		return route;
	}

	//TODO DEBUG
	private void colorizeEfficientPaths(Node end, Node begin) {
		PriorityQueue<Node> efficientNodes = new PriorityQueue<Node>(500, this.comparator);
		efficientNodes.add(end);

		HashSet<Node> excluded = new HashSet<Node>();

		boolean stillSearching = true;

		while (stillSearching){
			Node current = efficientNodes.poll();
			if (current == null)
				return;
			if (current.getId() == begin.getId())
				continue;
			if (excluded.contains(current))
				continue;

			DCLogitNodeData data = getData(current);

			ArrayList<DCLogitNodeData> tmps = data.getBackNodesData();
			HashSet<Node> backNodes = new HashSet<Node>();
			for (DCLogitNodeData tmp : tmps){
				Node back = this.network.getNode(tmp.id.toString());
				efficientNodes.add(back);
				backNodes.add(back);

			}
			Node from = this.network.getNode(data.id.toString());
			for (Link l : from.getInLinks().values()) {
				Node tmp = l.getFromNode();
				if (((LinkPainter)this.netStateWriter).linkAttribExist(l.getId()))
					excluded.add(tmp);

				if (backNodes.contains(tmp)) {
					((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.25);
				}
			}
		}

	}
//	TODO DEBUG
	private String getString(double dbl){
		java.text.DecimalFormat format = new java.text.DecimalFormat("#.00");
		return format.format(dbl);
	}


	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *
	 * @param outNode
	 *            The Node to be expanded.
	 * @param toNode
	 *            The target Node of the route.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	void relaxNode(final Node outNode, final Node toNode, final PriorityQueue<Node> pendingNodes) {

		DCLogitNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();

		for (Link l : outNode.getOutLinks().values()) {
			Node n = l.getToNode();
			addToPendingNodes(l, n, pendingNodes, currTime, currCost, outNode, toNode);
		}
	}

	/**
	 * Adds some parameters to the given Node then adds it to the set of pending
	 * nodes.
	 *
	 * @param l
	 *            The link from which we came to this Node.
	 * @param n
	 *            The Node to add to the pending nodes.
	 * @param pendingNodes
	 *            The set of pending nodes.
	 * @param currTime
	 *            The time at which we started to traverse l.
	 * @param currCost
	 *            The cost at the time we started to traverse l.
	 * @param outNode
	 *            The Node from which we came to n.
	 * @param toNode
	 *            The target Node of the route.
	 */
	boolean addToPendingNodes(final Link l, final Node n,
			final PriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost, final Node outNode, final Node toNode) {

		double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = this.costFunction.getLinkTravelCost(l, currTime);
		DCLogitNodeData data = getData(n);


		DCLogitNodeData fromNodeData = getData(outNode);

		//prevent u-turns ... just a HACK ... thats wrong ... u-turns has to be handled in backLinks!!!
		if ((fromNodeData.getBackNodes().size() == 1) && fromNodeData.getBackNodes().contains(n))
			return false;


		double trace = 0; // this.tracer.getTrace(fromNodeData.getTrace(), travelCost, n.getCoord());

		if (!data.isVisited(getIterationID())){


			data.resetVisited();
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode,trace);

			//TODO DEBUG
			((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.2);
			((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(data.getTrace()));

			return true;
		}
		double nCost = data.getCost();

		if (travelCost + currCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode,trace);

			//TODO DEBUG
			((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.4);
			((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(data.getTrace()));

			return true;
		}

//		DIAL efficient path depcrated
//		if (currCost < nCost){
////			if (Math.abs(data.getTrace() - trace) < 0.2*trace)
////				return false;
//			touchNode(n, data, pendingNodes, currTime + travelTime, currCost
//					+ travelCost, outNode,trace);
//
//			//TODO DEBUG
//			((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.25);
//			((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(data.getTrace()));
//
//			return true;
//
//		}

		//allow detour if not more then 10% longer - just a HACK
		if ((Math.abs(nCost  - (travelCost + currCost)) <= 0.2 * nCost) &&  (Math.abs(data.getTrace() - trace) > 30)){
			touchNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode,trace);
//			TODO DEBUG
			((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.6);
			((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(data.getTrace()));
			return true;
		}


		//TODO DEBUG
//		((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.99);
//		((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(data.getTrace()));

		return false;
	}

	/**
	 * Changes the position of the given Node n in the pendingNodes queue and
	 * updates its time and cost information.
	 *
	 * @param n
	 *            The Node that is revisited.
	 * @param data
	 *            The data for n.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param outNode
	 *            The node from which we came visiting n.
	 */
	void revisitNode(final Node n, final DCLogitNodeData data,
			final PriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Node outNode, final double trace) {
		/* PriorityQueueBucket.remove() uses the comparator given at instantiating
		 * to find the matching Object. This can lead to removing a wrong object
		 * which happens to have the same key for comparison, but is a completely
		 * different object... Thus we tell the comparator to check the IDs too if
		 * two objects are considered "equal" */
		this.comparator.setCheckIDs(true);
		pendingNodes.remove(n);
		this.comparator.setCheckIDs(false);

		data.visit(outNode, cost, time, getIterationID(),this.getData(outNode),trace);
		pendingNodes.add(n);
		this.revisitNodeCount++;
	}


		/**
	 * Augments the iterationID and checks whether the visited information in
	 * the nodes in the nodes have to be reset.
	 */
	protected void augmentIterationID() {
		if (getIterationID() == Integer.MAX_VALUE) {
			this.iterationID = Integer.MIN_VALUE + 1;
		} else {
			this.iterationID++;
		}

		checkToResetNetworkVisited();
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet if
	 * the iterationID is equal to Integer.MIN_VALUE + 1.
	 */
	void checkToResetNetworkVisited() {
		// If the re-planning id passed the maximal possible value
		// and has the same value as at the 'beginning', we reset all nodes of
		// the network to avoid identifying a node falsely as visited for the
		// current iteration
		if (getIterationID() == Integer.MIN_VALUE + 1) {
			resetNetworkVisited();
		}
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet.
	 */
	private void resetNetworkVisited() {
		for (Node node : this.network.getNodes().values()) {
			DCLogitNodeData data = getData(node);
			data.resetVisited();
		}
	}

	/**
	 * Initialises the first node of a route.
	 *
	 * @param fromNode
	 *            The Node to be initialized.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time we start routing.
	 * @param pendingNodes
	 *            The pending nodes so far.
	 */
	void initFromNode(final Node fromNode, final Node toNode, final double startTime,
			final PriorityQueue<Node> pendingNodes) {
		DCLogitNodeData data = getData(fromNode);
		data.resetVisited();
		data.visitInitNode(startTime, getIterationID());
		pendingNodes.add(fromNode);
		this.visitNodeCount++;
	}

	/**
	 * Inserts the given Node n into the pendingNodes queue and updates its time
	 * and cost information.
	 *
	 * @param n
	 *            The Node that is revisited.
	 * @param data
	 *            The data for n.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param outNode
	 *            The node from which we came visiting n.
	 * @param
	 */
	void visitNode(final Node n, final DCLogitNodeData data,
			final PriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Node outNode, final double trace) {
		data.visit(outNode, cost, time, getIterationID(),this.getData(outNode), trace);
		pendingNodes.add(n);
		this.visitNodeCount++;
	}

	void touchNode(final Node n, final DCLogitNodeData data,
			final PriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Node outNode, final double trace) {
		data.visit(outNode, cost, time, getIterationID(),this.getData(outNode), trace);
	}


	/**
	 * @return iterationID
	 */
	int getIterationID() {
		return this.iterationID;
	}


	/**
	 * Returns the data for the given node. Creates a new NodeData if none exists
	 * yet.
	 *
	 * @param n
	 *            The Node for which to return the data.
	 * @return The data for the given Node
	 */
	protected DCLogitNodeData getData(Node n) {
		DCLogitNodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new DCLogitNodeData(n.getId(),n.getOutLinks().size());
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}



	public static class ComparatorDCLogitCost implements Comparator<Node>, Serializable {

		private static final long serialVersionUID = 1L;

		private boolean checkIDs = false;

		protected Map<IdI, ? extends DCLogitNodeData> nodeData;

		public ComparatorDCLogitCost(Map<IdI, ? extends DCLogitNodeData> nodeData) {
			this.nodeData = nodeData;
		}

		public int compare(Node n1, Node n2) {
			double c1 = getKey(n1); // if a node
			double c2 = getKey(n2);

			return compare(n1, c1, n2, c2);
		}

		private int compare(Node n1, double c1, Node n2, double c2) {
			if (c1 < c2) {
				return -1;
			} else if (c1 == c2) {
				if (this.checkIDs) {
					return n1.compareTo(n2);
				}
				return 0;
			} else {
				return +1;
			}
		}

		public void setCheckIDs(boolean flag) {
			this.checkIDs = flag;
		}

		public double getKey(Node node) {
			return this.nodeData.get(node.getId()).getCost();
		}
	}

	static class DCLogitNodeData  {

		public final IdI id;

		private Node shortestPrev;

		private Node thisNode;

		private double cost = Double.MAX_VALUE;

		private double time = 0;

		private int iterationID = Integer.MIN_VALUE;

		private int inPaths = 0;

		public static double beta = 6.5;
		double backLinkWeightsSum = 0;

		//the efficient backpath nodes
//		PriorityQueue<DCLogitNodeData> prevNodes = new PriorityQueue<DCLogitNodeData>();
		private ConcurrentLinkedQueue<BackLink> backLinks = new ConcurrentLinkedQueue<BackLink>();

		private double trace = 0;

		private int visitCount =0;

		private int openLinks;

		public DCLogitNodeData(IdI id, int numOfOutLinks){
			this.id = id;
			this.openLinks = numOfOutLinks;
		}

		public boolean hasOpenLinks(){
			return this.openLinks > 0;
		}

		public ArrayList<DCLogitNodeData> getBackNodesData(){
			ArrayList<DCLogitNodeData> links = new ArrayList<DCLogitNodeData>();
			for (BackLink back : this.backLinks){
				links.add(back.fromNodeData);
			}
			return links;
		}

		public HashSet<Node> getBackNodes(){
			HashSet<Node> nodes = new HashSet<Node>();
			for (BackLink back : this.backLinks){
				nodes.add(back.fromNode);
			}
			return nodes;
		}


		public int getInPaths(IdI id){
			int inPaths = 0;
			if (this.backLinks.size() == 0)
				return 0;

			for (BackLink link : this.backLinks){
				if (link.fromNode.getId().equals(id))
					continue;
				inPaths += link.inPaths;
			}
			return inPaths;
		}

		public Node drawNode() {

			// choose a random number over interval [0,sumWeights[
			double selnum = this.backLinkWeightsSum*Gbl.random.nextDouble();
			for (BackLink link : this.backLinks){
				selnum -= link.linkWeight;
				if (selnum <= 0) {
					return link.fromNode;
				}
			}
			return null;
		}

		public double getTrace(){
			return this.trace;
		}

		public Node getPrevNode() {
			return this.shortestPrev;
		}

		public void setInPaths(int i){
			this.inPaths = i;
		}

		public boolean isVisited(int iterID) {
			return (iterID == this.iterationID);
		}

		//TODO the cost is also context sensitiv ... fix it!
		public double getCost() {
			return this.cost;
		}

		public double getTime() {
			return this.time;
		}

		public void resetVisited() {
			this.shortestPrev = null;
			this.iterationID = Integer.MIN_VALUE;
//			prevNodes.clear();
			this.backLinks.clear();
			this.cost = Double.MAX_VALUE;
			this.inPaths = 0;
			this.backLinkWeightsSum = 0;
		}

		public void visitInitNode(double time, int iterationID) {
			this.cost = 0;
			this.time = time;
			this.iterationID = iterationID;
			this.inPaths = 1;
//			BackLink blink = new BackLink();
//			blink.linkWeight = 1;
//			backLinks.add(blink);

		}

		public void visit(Node outNode, double cost, double time, int iterationID, DCLogitNodeData fromNodeData, double trace) {

//			System.out.print("old:" + this.trace + " inp:" + this.inPaths + " input:" + trace);
			this.trace = (this.trace * this.visitCount + trace)/(this.visitCount +1);
//			System.out.println(" new " + this.trace);

			if (cost < this.cost) {
				for (BackLink link : this.backLinks) {
					if (Math.abs(link.trace - trace) <= 0.5 * trace){
						this.backLinks.remove();
						break; //TODO test the other links too
					}
				}
				this.openLinks--;
				this.shortestPrev = outNode;
				this.cost = cost;
				this.time = time;
				this.iterationID = iterationID;
			}
			this.visitCount ++;
			this.inPaths += fromNodeData.getInPaths(this.id);
//			prevNodes.add(prevData);
			double linkCost = cost - fromNodeData.getCost();
//			double overlap = getOverlap(fromNodeData.getInPaths(), fromNodeData.getCost(),linkCost);
//			double linkLikelihood = Math.exp(- linkCost - overlap);
//			double tmp = 0;
//			for (BackLink bl : fromNodeData.backLinks)
//				tmp += bl.linkWeight;
//
//			double linkWeight = linkLikelihood * tmp;
//			backLinkWeightsSum += linkWeight;
			BackLink blink = new BackLink();
			blink.fromNodeData = fromNodeData;
			blink.trace = trace;
//			blink.linkWeight = 1; linkWeight;
			blink.fromNode = outNode;
			blink.linkCost = linkCost;
			blink.inPaths = fromNodeData.getInPaths(this.id);
			this.backLinks.add(blink);
		}


		private void calcLinkWeights(){
//			TODO ....
		}

		private double getOverlap(int inPaths, double prevCost, double linkCost) {
			return beta * Math.log(inPaths) * linkCost / (linkCost + prevCost);
		}

		//comparator for the back-path
		public int compareTo(Object o) {
			if (this.getCost() < ((DCLogitNodeData) o).getCost())
				return 1;
			if (this.getCost() > ((DCLogitNodeData) o).getCost())
				return -1;
			return 0;
		}

		private class BackLink {
			DCLogitNodeData fromNodeData;
			double linkWeight;
			double inPaths;
			Node fromNode;
			double linkCost;
			double trace;
		}
	}
}
