/* *********************************************************************** *
 * project: org.matsim.*
 * ProbabilsticShortestPath.java
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

package playground.gregor.multipath.bckp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

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
import org.matsim.utils.vis.routervis.RouterNetStateWriter;
import org.matsim.utils.vis.routervis.VisLeastCostPathCalculator;

import playground.gregor.multipath.bckp.NodeData.ComparatorNodeData;
import playground.gregor.vis.LinkPainter;

/**
 * @author laemmel
 *
 */
public class ProbabilsticShortestPath implements LeastCostPathCalculator, VisLeastCostPathCalculator{


	/**
	 * The limit a path could be more expensive then the shortest path
	 */
	final static double OUTPRICED_CRITERION = 1.5;

	/**
	 * The network on which we find routes.
	 */
	final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	protected final TravelCostI costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTimeI timeFunction;


	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */
	protected ComparatorNodeData comparator;


	final private HashMap<IdI, NodeData> nodeData;

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


	//TODO DEBUGGING STUFF
	private  boolean debug = true;
	protected RouterNetStateWriter netStateWriter = null;
	private int time;


	public ProbabilsticShortestPath(NetworkLayer network, TravelCostI costFunction, TravelTimeI timeFunction, RouterNetStateWriter writer){
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<IdI, NodeData>((int)(network.getNodes().size() * 1.1), 0.95f);
		this.comparator = new ComparatorNodeData(this.nodeData);

		//TODO DEBUGGING STUFF
		if (debug){
//			initSnapShotWriter();
			this.netStateWriter = writer;
			this.time = 0;			
			this.debug = false;
		}


	}



	public Route calcLeastCostPath(Node fromNode, Node toNode, double startTime) {

		PriorityQueue<NodeData> pendingNodes = new PriorityQueue<NodeData>(500, this.comparator);

		double minCost = Double.MAX_VALUE;
		
		double arrivalTime = 0;

		this.tracer = new BeelineDifferenceTracer(fromNode.getCoord(), toNode.getCoord());

		// The forward path - spans the Dijkstra like shortest path tree

		boolean notCali = true;
		

		while (notCali) {

			boolean foundRoute = false;
			boolean stillSearching = true;

			augmentIterationID();

			initFromNode(fromNode, toNode, startTime, pendingNodes);

			int snapShotSlowDown = 50;
			int count = 0;

			while (stillSearching) {

				//			//TODO DEBUG
//				if (count++ >= snapShotSlowDown) {
//				try {
//				count = 0;
//				this.netStateWriter.dump(time++);
////				if (time > 600){
////				try {
////				this.netStateWriter.close();
////				} catch (IOException e) {
////				e.printStackTrace();
////				}
////				this.netStateWriter = null;
////				}
//				} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				}
//				}

				NodeData outNodeD = pendingNodes.poll();


				if (outNodeD == null) {
					if (foundRoute){
						break;
					}
					Gbl.warningMsg(this.getClass(), "calcLeastCostPath()",
							"No route was found from node " + fromNode.getId()
							+ " to node " + toNode.getId());
					return null;
				}

				if (outNodeD.getId() == toNode.getId()){
					foundRoute = true;
					minCost = Math.min(minCost, outNodeD.getSortCost());
					if (outNodeD.getSortCost()/minCost >= OUTPRICED_CRITERION) {
						stillSearching = false;
					} else {
						continue;
					}
				} else {
					relaxNode(outNodeD,pendingNodes);
				}

			}
			
			if (getData(toNode).getInPaths() >= 5){
				System.out.println("to many routes found:" + getData(toNode).getInPaths() + " decreasing sim crit ..." );
				this.netStateWriter.reset();
				this.tracer.decreaseCrit();
//				
//				try {
//					this.netStateWriter.dump(time++);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
////				((LinkPainter)this.netStateWriter).reset();
//				this.netStateWriter.reset();
				colorizeEfficientPaths(getData(toNode),getData(fromNode));
				try {
					this.netStateWriter.dump(time++);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
			} else if (getData(toNode).getInPaths() == 0) {
				System.out.println("no enough routes found:" + getData(toNode).getInPaths() + " increasing sim crit ..." );
				this.tracer.increaseCrit();				
				
			} else {
				notCali = false;
				this.debug = true;
			}

		}
		//TODO DEBUG
		if (debug){
			try {
				this.netStateWriter.dump(time++);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			((LinkPainter)this.netStateWriter).reset();
			this.netStateWriter.reset();
			colorizeEfficientPaths(getData(toNode),getData(fromNode));
			try {
				this.netStateWriter.dump(time++);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO DEBUG
			try {
				this.netStateWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.netStateWriter = null;

		}


		ArrayList<Node> routeNodes = new ArrayList<Node>();
		NodeData tmpNode = getData(toNode);
		double cost  = 0;
		while (tmpNode.getId() != fromNode.getId()) {
			routeNodes.add(0, tmpNode.getMatsimNode());
			tmpNode = tmpNode.drawNode();

		}
		routeNodes.add(0, tmpNode.getMatsimNode()); // add the fromNode at the beginning of the list

		Route route = new Route();
		route.setRoute(routeNodes, (int) (arrivalTime - startTime), cost);

		if (this.doGatherInformation) {
			this.avgTravelTime = (this.routeCnt * this.avgTravelTime + route
					.getTravTime()) / (this.routeCnt + 1);
			this.avgRouteLength = (this.routeCnt * this.avgRouteLength + route
					.getDist()) / (this.routeCnt + 1);
			this.routeCnt++;
		}
		return route;

	}

	//////////////////////////////////////////////////////////////////////
	// all the init stuff and helper methods starts here
	//////////////////////////////////////////////////////////////////////
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
	 * @return iterationID
	 */
	private int getIterationID() {
		return this.iterationID;
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
			final PriorityQueue<NodeData> pendingNodes) {
		NodeData data = getData(fromNode);
		data.resetVisited(this.tracer);
		data.visitInitNode(startTime, getIterationID());
		pendingNodes.add(data);
		this.visitNodeCount++;
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
			NodeData data = getData(node);
			data.resetVisited(this.tracer);
		}
	}

	/**
	 * Returns the data for the given node. Creates a new NodeData if none exists
	 * yet.
	 *
	 * @param n
	 *            The Node for which to return the data.
	 * @return The data for the given Node
	 */
	protected NodeData getData(Node n) {
		NodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new NodeData(n,this.tracer);
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}

	//////////////////////////////////////////////////////////////////////
	// the network exploration stuff starts here
	//////////////////////////////////////////////////////////////////////
	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *
	 * @param outNode
	 *            The NodeData to be expanded.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	void relaxNode(final NodeData outNodeD,final PriorityQueue<NodeData> pendingNodes) {


		double currTime = outNodeD.getTime();
		double currCost = outNodeD.getCost();


		for (Link l : outNodeD.getMatsimNode().getOutLinks().values()) {
			Node n = l.getToNode();

			//prevent u-turn exploration
			if (outNodeD.isUTurn(n)) {
//				handleUTurn(l, n, currTime, outNodeD);
				continue;
			}


			addToPendingNodes(l, n, pendingNodes, currTime, currCost,
					outNodeD);
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
	 * @param fromNodeData
	 *            The NodeData from which we came to n.
	 */
	private boolean addToPendingNodes(final Link l, final Node n, final PriorityQueue<NodeData> pendingNodes, final double currTime, final double currCost, final NodeData fromNodeData) {

		double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = this.costFunction.getLinkTravelCost(l, currTime);

		NodeData toNodeData = getData(n);
		double trace = this.tracer.getTrace(fromNodeData.getTrace(),fromNodeData.getMatsimNode().getCoord(), l.getLength(), n.getCoord());



		if (!toNodeData.isVisited(getIterationID())){


			toNodeData.resetVisited(this.tracer);
			visitNode(toNodeData, pendingNodes, currTime + travelTime, currCost
					+ travelCost, fromNodeData,trace);

			//TODO DEBUG
			if (debug){
//				((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.3);
//				((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(toNodeData.getTrace()) + "  -  " + getString(travelCost+currCost));
				this.netStateWriter.setLinkColor(l.getId(), 0.3);
				this.netStateWriter.setLinkMsg(l.getId(), getString(toNodeData.getTrace()) + "  -  " + getString(travelCost+currCost));
			}


			return true;
		}
		double nCost = toNodeData.getCost();
		if (travelCost + currCost < nCost) {

			//TODO DEBUG ... make revisit node void ...
			if(revisitNode(toNodeData, pendingNodes, currTime + travelTime, currCost
					+ travelCost, fromNodeData,trace)) {

				//TODO DEBUG
				if (debug) {
					this.netStateWriter.setLinkColor(l.getId(), 0.5);
					this.netStateWriter.setLinkMsg(l.getId(), getString(toNodeData.getTrace()) + "  -  " + getString(travelCost+currCost));
				}
			} else {
				//TODO DEBUG
				if (debug){
					System.err.println("doch passiert?");
				    netStateWriter.setLinkColor(l.getId(), 0.99);
					this.netStateWriter.setLinkMsg(l.getId(), getString(toNodeData.getTrace()) + "  -  " + getString(travelCost+currCost));
				}
			}
			return true;
		}

		if (this.tracer.tracesDiffer(trace, toNodeData.getTrace()) && ((travelCost + currCost) / nCost < OUTPRICED_CRITERION)){
			//TODO do somthing ....
			if (trackPath(toNodeData, pendingNodes, currTime + travelTime, currCost + travelCost, fromNodeData, trace, travelCost)){
				//TODO DEBUG
				if (debug){
//					((LinkPainter)this.netStateWriter).setLinkColor(l.getId(), 0.80);
//					((LinkPainter)this.netStateWriter).setLinkMsg(l.getId(), getString(toNodeData.getCurrShadow().getTrace()));
					this.netStateWriter.setLinkColor(l.getId(), 0.80);
					this.netStateWriter.setLinkMsg(l.getId(), getString(toNodeData.getCurrShadow().getTrace()));
				}
			} else {
				//TODO DEBUG
				if (debug){
					this.netStateWriter.setLinkColor(l.getId(), 0.10);
					this.netStateWriter.setLinkMsg(l.getId(), getString(toNodeData.getTrace()));
				}
			}

		}

		//TODO
		return false;
	}

//	/**
//	* Generates the forwardLinks in fromNodeData if an u-turn was detected. These links are
//	* needed for trackPath
//	*
//	* @param l
//	*            The link from which we came to this Node.
//	* @param n
//	*            The Node to add to the pending nodes.
//	* @param currTime
//	*            The time at which we started to traverse l.
//	* @param fromNodeData
//	*            The NodeData from which we came to n.
//	*/
//	private void handleUTurn(Link l, Node n, double currTime, NodeData fromNodeData)  {
//	double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
//	double travelCost = this.costFunction.getLinkTravelCost(l, currTime);
//	double trace = this.tracer.getTrace(fromNodeData.getTrace(),fromNodeData.getMatSimNode().getCoord(), travelCost, n.getCoord());
//	fromNodeData.createForwardLinks(getData(n), travelCost, travelTime, trace);
//	}


	/**
	 * Inserts the given NodeData toNodeData into the pendingNodes queue and updates its time
	 * and cost information.
	 *
	 * @param toNodeData
	 *            The NodeData
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param fromNodeData
	 *            The NodeData from which we came visiting toNodeData.
	 */
	private void visitNode(final NodeData toNodeData, final PriorityQueue<NodeData> pendingNodes, final double time, final double cost, final NodeData fromNodeData, final double trace) {
		toNodeData.visit(fromNodeData, cost, time, getIterationID(), trace);
		pendingNodes.add(toNodeData);
		this.visitNodeCount++;
	}

	/**
	 * Revisits the given NodeData toNodeData and place it into the pendingNodes queue and updates its time
	 * and cost information.
	 *
	 * @param toNodeData
	 *            The NodeData
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param fromNodeData
	 *            The NodeData from which we came visiting toNodeData.
	 */
	private boolean revisitNode(final NodeData toNodeData, final PriorityQueue<NodeData> pendingNodes, final double time, final double cost, final NodeData fromNodeData, final double trace) {

		for (NodeData backNode : toNodeData.getBackNodesData()){
			NodeData shadowNode = new NodeData(toNodeData.getMatsimNode(),this.tracer);
			shadowNode.visit(backNode, toNodeData.getCost(), toNodeData.getTime(), iterationID, toNodeData.getTrace());
			toNodeData.decoupleNode(backNode);
			pendingNodes.add(shadowNode);
		}

		if (toNodeData.visit(fromNodeData, cost, time, getIterationID(),trace)){
			/* PriorityQueueBucket.remove() uses the comparator given at instantiating
			 * to find the matching Object. This can lead to removing a wrong object
			 * which happens to have the same key for comparison, but is a completely
			 * different object... Thus we tell the comparator to check the IDs too if
			 * two objects are considered "equal" */
			this.comparator.setCheckIDs(true);
			pendingNodes.remove(toNodeData);
			this.comparator.setCheckIDs(false);
			pendingNodes.add(toNodeData);
			this.revisitNodeCount++;
		} else {
			return false;
		}
		return true;
	}



	/**
	 * Track the path to the exploration head and create pseudo nodes for the new feasible path.
	 *
	 * @param toNodeData
	 *            The NodeData
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param fromNodeData
	 *            The NodeData from which we came visiting toNodeData.
	 */
	private boolean trackPath(final NodeData toNodeData, final PriorityQueue<NodeData> pendingNodes, final double time, final double cost, final NodeData fromNodeData, final double trace, final double linkCost) {


		NodeData shadowNode = new NodeData(toNodeData.getMatsimNode(),this.tracer);

		shadowNode.visit(fromNodeData, cost, time, iterationID, trace);
		shadowNode.setSortCost(toNodeData.getCost());
		if (!testShadowNode(shadowNode,toNodeData)) return false;
		toNodeData.addShadowNode(shadowNode);
		/* PriorityQueueBucket.remove() uses the comparator given at instantiating
		 * to find the matching Object. This can lead to removing a wrong object
		 * which happens to have the same key for comparison, but is a completely
		 * different object... Thus we tell the comparator to check the IDs too if
		 * two objects are considered "equal" */
		this.comparator.setCheckIDs(true);
		pendingNodes.remove(toNodeData);
		this.comparator.setCheckIDs(false);
		pendingNodes.add(shadowNode);



		return true;
	}

	/**
	 * If toNodeData is the head of the exploration, then it will be visited and the method returns false.
	 * Else, it tests weather, there is another shadowNode with an equal trace.
	 * Returns true if there is no shadowNode with an equal trace or all
	 * equal shadowNodes are more expensiv (in this case the corresponding
	 * shadows will be deleted)
	 * @param shadowNode
	 * @param toNodeData
	 * @return success
	 */
	private boolean testShadowNode(NodeData shadowNode, NodeData toNodeData) {


//		if ((shadowNode.getCost() / toNodeData.getCost()) >= OUTPRICED_CRITERION)
//		return false;

		if (toNodeData.isHead()){

			toNodeData.visit(shadowNode.getBackNodesData().peek(), shadowNode.getCost(), shadowNode.getTime(), this.iterationID, shadowNode.getTrace());
			toNodeData.setSortCost(shadowNode.getCost());
			return false;
		}

		// test if there is a cheaper equal node
		boolean equal = false;

		for (NodeData tmp : toNodeData.getShadowNodes()){
			if (!this.tracer.tracesDiffer(shadowNode.getTrace(), tmp.getTrace())){
				if (tmp.getCost() < shadowNode.getCost()) return false;
				else equal = true;
			}
		}
		if (equal == true){
			for (NodeData tmp : toNodeData.getShadowNodes()){
				if (!this.tracer.tracesDiffer(shadowNode.getTrace(), tmp.getTrace())){
					toNodeData.getShadowNodes().remove(tmp);
				}
			}
		}
		return true;
	}

	//	TODO DEBUGGING STUFF
//	private void initSnapShotWriter() {
//
//		String snapshotFile = "./output/Snapshot";
//
//		Config config = Gbl.getConfig();
//		// OR do it like this: buffers = Integer.parseInt(Config.getSingleton().getParam("temporal", "buffersize"));
//		// Automatic reasoning about buffersize, so that the file will be about 5MB big...
//		int buffers = this.network.getLinks().size();
//		String buffString = config.findParam("vis", "buffersize");
//		if (buffString == null) {
//			buffers = Math.max(5, Math.min(50000/buffers, 100));
//		} else buffers = Integer.parseInt(buffString);
//
//		// Override LinkRenderSet if necessary
//
//		this.netStateWriter = new LinkPainter(this.network, config.network().getInputFile(), snapshotFile, 1, buffers);
//		this.netStateWriter.open();
//
//	}
	//	TODO DEBUGGING STUFF
	private String getString(double dbl){
		java.text.DecimalFormat format = new java.text.DecimalFormat("#.0000");
		return format.format(dbl);
	}
//	TODO DEBUG
	protected void colorizeEfficientPaths(final NodeData end, final NodeData begin) {
		PriorityQueue<NodeData> efficientNodes = new PriorityQueue<NodeData>(500, this.comparator);

		System.out.println("colorize");
		end.addNodeProb(1);
		efficientNodes.add(end);

		HashSet<Node> excluded = new HashSet<Node>();

		boolean stillSearching = true;

		int maxPath = 0 ;

		while (stillSearching){
			NodeData current = efficientNodes.poll();
			if (current == null) {
				System.out.println(maxPath);
				return;
			}
			if (current.getId() == begin.getId())
				continue;
			if (excluded.contains(current.getMatsimNode()))
				continue;

			if(current.getMatsimNode().getId().toString().equals("101500010")){
				int i = 0; i++;
			}

//			LinkedList<NodeData> tmps = (LinkedList<NodeData>) current.getBackNodesData();
			Iterator it = current.getBackNodesData().iterator();
			HashSet<Node> backNodes = new HashSet<Node>();
			while (it.hasNext()){
				NodeData tmp = (NodeData) it.next();
				Node back = tmp.getMatsimNode();
//				tmp.setSortCost(1/tmp.getCost());
				efficientNodes.add(tmp);
				backNodes.add(back);

			}
			Node from = current.getMatsimNode();
//			this.netStateWriter.setNodeMsg(from.getId(), getString(current.getCost()));
			
			current.computeProbs();

			for (Link l : from.getInLinks().values()) {
				Node tmp = l.getFromNode();
//				if (((LinkPainter)this.netStateWriter).linkAttribExist(l.getId()))
//				excluded.add(tmp);

				if (backNodes.contains(tmp)) {
					this.netStateWriter.setLinkColor(l.getId(), 0.1);
					double prob = current.getNodeProb(tmp);
					this.netStateWriter.setLinkMsg(l.getId(), current.getInPaths() + "     " +  getString(prob));
					maxPath = Math.max(maxPath, current.getInPaths());
				}
			}
		}
	}

}
