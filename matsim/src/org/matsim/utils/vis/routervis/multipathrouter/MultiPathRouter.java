/* *********************************************************************** *
 * project: org.matsim.*
 * LogitRouter.java
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

package org.matsim.utils.vis.routervis.multipathrouter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.vis.routervis.RouterNetStateWriter;
import org.matsim.utils.vis.routervis.VisLeastCostPathCalculator;
import org.matsim.utils.vis.routervis.multipathrouter.NodeData.ComparatorNodeData;

abstract class MultiPathRouter implements VisLeastCostPathCalculator{
	
	private static final Logger log = Logger.getLogger(MultiPathRouter.class);
	
	/**
	 * The limit a path could be more expensive then the shortest path
	 */
	final static double OUTPRICED_CRITERION = 1.3;

	/**
	 * The network on which we find routes.
	 */
	final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	protected final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTime timeFunction;


	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */
	protected ComparatorNodeData comparator;


	final private HashMap<Id, NodeData> nodeData;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;
	
	private int shadowID;

	private BeelineDifferenceTracer tracer;
	
	protected LogitSelector selector;	
	//TODO [gl] DEBUGGING STUFF
	private final  boolean debug = true;
	protected RouterNetStateWriter netStateWriter = null;

	private int dumpCounter;

	public MultiPathRouter(final NetworkLayer network, final TravelCost costFunction, final TravelTime timeFunction, final RouterNetStateWriter writer){
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<Id, NodeData>((int)(network.getNodes().size() * 1.1), 0.95f);
		this.comparator = new ComparatorNodeData(this.nodeData);

		initSelector();
		
		//TODO [gl] DEBUGGING STUFF
		if (this.debug){
			this.netStateWriter = writer;
			this.dumpCounter = 0;
		}
	
}

	
	abstract void initSelector();

	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {
		final PriorityQueue<NodeData> pendingNodes = new PriorityQueue<NodeData>(500, this.comparator);

		double minCost = Double.MAX_VALUE;
		
		this.shadowID =  Integer.MIN_VALUE + 1;

		final double arrivalTime = 0;

		this.tracer = new BeelineDifferenceTracer(fromNode.getCoord(), toNode.getCoord());

		// The forward path - spans the Dijkstra like shortest path tree

		final ArrayList<NodeData> toNodes = new ArrayList<NodeData>();

		
//			int count = 0;
			boolean foundRoute = false;
			boolean stillSearching = true;

			augmentIterationID();

			initFromNode(fromNode, toNode, startTime, pendingNodes);


			while (stillSearching) {

				final NodeData outNodeD = pendingNodes.poll();


				if (outNodeD == null || (outNodeD.getCost()/minCost >= OUTPRICED_CRITERION)) {
					if (foundRoute){
						break;
					}
					return null;
				}



//				if (this.debug) {
//					if (count++ >= this.slowDown){
//						count = 0;
//						doSnapshot();
//					}					
//				}


				if (outNodeD.getId() == toNode.getId()){
					toNodes.add(outNodeD);
					foundRoute = true;
					minCost = Math.min(minCost, outNodeD.getCost());
					if (outNodeD.getCost()/minCost >= OUTPRICED_CRITERION) {
						stillSearching = false;
					} else {
						continue;
					}
				} else {
					
					if (!outNodeD.isShadow()) {
//						this.netStateWriter.setNodeMsg(outNodeD.getMatsimNode().getId(), "" + outNodeD.getShadowNodes().size());
						relaxNode(outNodeD,pendingNodes);	
						
					} else {
						relaxShadowNode(outNodeD,pendingNodes);
					}
					
				}

			}


	
		this.selector.run(toNodes,getData(fromNode));
		//TODO [gl] DEBUG
		colorizePaths(toNodes,fromNode);
		
		if (true)    // that's a joke, isn't it? // marcel,2008nov28
		return null;
		
		final ArrayList<Node> routeNodes = new ArrayList<Node>();
		NodeData tmpNode = getData(toNode);
		final double cost  = 0;
		while (tmpNode.getId() != fromNode.getId()) {
			routeNodes.add(0, tmpNode.getMatsimNode());
			tmpNode = tmpNode.drawNode();

		}
		routeNodes.add(0, tmpNode.getMatsimNode()); // add the fromNode at the beginning of the list

		final Path path = new Path(routeNodes, null, (int) (arrivalTime - startTime), cost); // FIXME [MR] collect links

		return path;
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


		final double currTime = outNodeD.getTime();
		final double currCost = outNodeD.getCost();
		
		outNodeD.setHead(false);

		for (final Link l : outNodeD.getMatsimNode().getOutLinks().values()) {
			final Node n = l.getToNode();

			//prevent u-turn exploration
			if (outNodeD.isUTurn(n)) {

				continue;
			}
			addToPendingNodes(l, n, pendingNodes, currTime, currCost, outNodeD);
		}
	}
	

	/**
	 * Expands the given Node in the routing algorithm as an shadow node; may be overridden in
	 * sub-classes.
	 *
	 * @param outNodeD
	 *            The NodeData to be expanded.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	private void relaxShadowNode(final NodeData outNodeD,final PriorityQueue<NodeData> pendingNodes) {
		final double currTime = outNodeD.getTime();
		final double currCost = outNodeD.getCost();
		
		outNodeD.setHead(false);

		for (final Link l : outNodeD.getMatsimNode().getOutLinks().values()) {
			final Node n = l.getToNode();

			//prevent u-turn exploration
			if (outNodeD.isUTurn(n)) {

				continue;
			}
			addShadowToPendingNodes(l, n, pendingNodes, currTime, currCost, outNodeD);
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
	private void addToPendingNodes(final Link l, final Node n, final PriorityQueue<NodeData> pendingNodes, final double currTime, final double currCost, final NodeData fromNodeData) {

		final double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		final double travelCost = this.costFunction.getLinkTravelCost(l, currTime);

		final NodeData toNodeData = getData(n);
		final double trace = this.tracer.getTrace(fromNodeData.getTrace(),fromNodeData.getMatsimNode().getCoord(), l.getLength(), n.getCoord());

		if (!toNodeData.isVisited(getIterationID())){
			visitNode(toNodeData, pendingNodes, currTime + travelTime, currCost + travelCost, fromNodeData,trace);
			this.netStateWriter.setLinkColor(l.getId(), 0.1);
		}
		final double nCost = toNodeData.getCost();
		if (travelCost + currCost < nCost) {
			revisitNode(toNodeData, pendingNodes, currTime + travelTime, currCost + travelCost, fromNodeData,trace);
			this.netStateWriter.setLinkColor(l.getId(), 0.4);
		}

		//accept node a second time ...
		if ( ((travelCost + currCost) / nCost < OUTPRICED_CRITERION) && this.tracer.tracesDiffer(trace, toNodeData.getTrace())){
			touchNode(toNodeData, pendingNodes, currTime + travelTime, currCost + travelCost, fromNodeData,trace);
		}




	}

	/**
	 * Adds some parameters to the given Node, generates a new Shadow Node then adds it to the set of pending
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
	private void addShadowToPendingNodes(final Link l, final Node n,	final PriorityQueue<NodeData> pendingNodes, final double currTime, final double currCost, final NodeData fromNodeData) {
		final double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		final double travelCost = this.costFunction.getLinkTravelCost(l, currTime);

		final NodeData toNodeData = getData(n);
		final double shadowCost = travelCost + currCost;
		
		if (shadowCost/toNodeData.getCost() > OUTPRICED_CRITERION) {
			log.info("route to expensive - giving up");
			return;
		}
		if (toNodeData.containsShadowNode(fromNodeData.getShadowID())) {
			log.info("circular explorations not allowed!");
			return;
		}
		
		final double trace = this.tracer.getTrace(fromNodeData.getTrace(),fromNodeData.getMatsimNode().getCoord(), l.getLength(), n.getCoord());
		
		final ArrayList<NodeData> toDelete = new ArrayList<NodeData>();
		for (final NodeData temp : toNodeData.getShadowNodes())  {
			if (!fromNodeData.containsShadowNode(temp.getShadowID())) {
						if (!this.tracer.tracesDiffer(trace, temp.getTrace())) {
						if (temp.getCost() < shadowCost) {
							log.info("there is already a similar shadow node with lower costs - giving up");
							return;					
						} else {
							toDelete.add(temp);
						}
					}
				}
		}
		for (final NodeData del : toDelete) {
			toNodeData.rmShadow(del);
		}
		
		final NodeData shadow = new NodeData(n,true);
		shadow.visitShadow(fromNodeData, currCost + travelCost, currTime + travelTime, this.iterationID, trace,fromNodeData.getShadowID());
		toNodeData.addShadow(shadow);
		pendingNodes.add(shadow);
		
	}


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
	private void revisitNode(final NodeData toNodeData, final PriorityQueue<NodeData> pendingNodes, final double time, final double cost, final NodeData fromNodeData, final double trace) {

		pendingNodes.add(toNodeData.getPrev());
		for (final NodeData n : toNodeData.getShadowNodes()) {
			pendingNodes.add(n);
		}

		toNodeData.reset();
		toNodeData.visit(fromNodeData, cost, time, getIterationID(),trace);
		/* PriorityQueueBucket.remove() uses the comparator given at instantiating
		 * to find the matching Object. This can lead to removing a wrong object
		 * which happens to have the same key for comparison, but is a completely
		 * different object... Thus we tell the comparator to check the IDs too if
		 * two objects are considered "equal" */
		this.comparator.setCheckIDs(true);
		pendingNodes.remove(toNodeData);
		this.comparator.setCheckIDs(false);
		pendingNodes.add(toNodeData);
	}

	/**
	 * Visits the given NodeData toNodeData a second time and explore the graph a second time as long
	 * as the "OUTPRICED" constraint holds
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
	private void touchNode(final NodeData toNodeData, final PriorityQueue<NodeData> pendingNodes, final double time, final double cost, final NodeData fromNodeData, final double trace) {

		final ArrayList<NodeData> toDelete = new ArrayList<NodeData>();
		for (final NodeData temp : toNodeData.getShadowNodes()) {
			if (!this.tracer.tracesDiffer(trace, temp.getTrace())) {
				if (temp.getCost() < cost) {
					log.info("there is already a similar shadow node with lower costs - giving up");
					return;					
				} else {
					toDelete.add(temp);
				}
			}
		}
		for (final NodeData del : toDelete) {
			toNodeData.rmShadow(del);
		}
		final NodeData shadow = new NodeData(toNodeData.getMatsimNode(),true);
		shadow.visitShadow(fromNodeData, cost, time, this.iterationID, trace, this.shadowID++);
		toNodeData.addShadow(shadow);
		pendingNodes.add(shadow);
		
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
		final NodeData data = getData(fromNode);
		data.reset();
		data.visitInitNode(startTime,this.iterationID);
		pendingNodes.add(data);
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
		for (final Node node : this.network.getNodes().values()) {
			final NodeData data = getData(node);
			data.reset();
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
	protected NodeData getData(final Node n) {
		NodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new NodeData(n, false);
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}
	
	
	//RouterVis - stuff
	
	private void colorizePaths(final ArrayList<NodeData> toNodes, final Node fromNode) {
		
		log.info("found " + toNodes.size() + " paths!");
		this.netStateWriter.reset();
		doSnapshot();
		double color = 0.1;
			for (NodeData node : toNodes) {
				final double prob = node.getProb();
				while (node.getId() != fromNode.getId()) {
					for (final Link l : node.getMatsimNode().getInLinks().values()) {
						if (l.getFromNode().getId() == node.getPrev().getId()) {
							this.netStateWriter.setLinkColor(l.getId(), color);
							this.netStateWriter.setLinkMsg(l.getId(), getString(prob));
//							this.netStateWriter.setNodeMsg(l.getFromNode().getId(), );
							break;
						}
						
					}
					node = node.getPrev();
				}
				doSnapshot();
				color += 0.1;
			}			
		
	}
	
	
	private String getString(final double dbl){
		final java.text.DecimalFormat format = new java.text.DecimalFormat("#.0000");
		return format.format(dbl);
	}
	
	private void doSnapshot() {
		try {
			this.netStateWriter.dump(this.dumpCounter++);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}