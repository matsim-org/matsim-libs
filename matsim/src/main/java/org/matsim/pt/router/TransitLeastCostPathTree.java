/* *********************************************************************** *
 * project: org.matsim.*
 * TransitDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * This class is based on and similar to org.matsim.pt.router.MultiNodeDijkstra
 *
 * In contrast to org.matsim.pt.router.MultiNodeDijkstra, however, it stores the last
 * LeastCostPathTree. It is, therefore, much faster than it, in cases where many routes
 * starting with the same fromNode are calculated subsequently as every route from that
 * fromCoord can be retrieved now without having to compute the tree again.
 *
 * Call createLeastCostPathTree(fromNodes, person, fromCoord) in order to create and cache the
 * LeastCostPathTree. Every following call of getPath(toNodes) will return the leastCostPath
 * from the fromNodes to the toNodes.
 *
 * The fromCoord you passed in createLeastCostPathTree operates as a primary key to determine
 * if you are working with the LeastCostPathTree you think you are. You can just pass the
 * original requested origin here.
 *
 * Please keep in mind, that you are responsible to order all queries in a way such that all
 * those originating in the same fromCoord are done subsequently. Otherwise, the mentioned
 * efficiency gain with not take effect.
 *
 * @author gthunig
 */
public class TransitLeastCostPathTree {

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	private final TransitTravelDisutility costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	private final TravelTime timeFunction;

	private final HashMap<Id<Node>, DijkstraNodeData> nodeData;
	private Person person = null;
	private Vehicle vehicle = null;
	private CustomDataManager customDataManager = new CustomDataManager();
	private Map<Node, InitialNode> fromNodes = null;

	private RouterPriorityQueue<Node> pendingNodes;

	public TransitLeastCostPathTree(final Network network, final TransitTravelDisutility costFunction,
									final TravelTime timeFunction,
									final Map<Node, InitialNode> fromNodes, final Person person) {
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<>((int)(network.getNodes().size() * 1.1), 0.95f);

		//create tree
		this.resetNetworkVisited();
		this.person = person;
		this.customDataManager.reset();
		this.fromNodes = fromNodes;

		pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
		for (Map.Entry<Node, InitialNode> entry : fromNodes.entrySet()) {
			DijkstraNodeData data = getData(entry.getKey());
			visitNode(entry.getKey(), data, pendingNodes, entry.getValue().initialTime, entry.getValue().initialCost, null);
		}

		// do the real work
		while (pendingNodes.size() > 0) {
			Node outNode = pendingNodes.poll();
			relaxNode(outNode, pendingNodes);
		}
	}

	public TransitLeastCostPathTree(final Network network, final TransitTravelDisutility costFunction,
									final TravelTime timeFunction,
									final Map<Node, InitialNode> fromNodes, final Map<Node, InitialNode> toNodes,
									final Person person) {
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<>((int)(network.getNodes().size() * 1.1), 0.95f);

		//create tree
		this.resetNetworkVisited();
		this.person = person;
		this.customDataManager.reset();
		this.fromNodes = fromNodes;

		pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
		for (Map.Entry<Node, InitialNode> entry : fromNodes.entrySet()) {
			DijkstraNodeData data = getData(entry.getKey());
			visitNode(entry.getKey(), data, pendingNodes, entry.getValue().initialTime, entry.getValue().initialCost, null);
		}

		expandNodeData(toNodes);
	}

	private int getIterationId() {
//		TODO could delete all the now unnecessary occurrences of the IterationID especially in DijkstraNodeData and
//		TODO replace it with a flag visited but that would interfere the useage of the MultiNodeDijkstra
		return 0;
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet.
	 */
	private void resetNetworkVisited() {
		for (Node node : this.network.getNodes().values()) {
			DijkstraNodeData data = getData(node);
			data.resetVisited();
		}
	}

	private void expandNodeData(final Map<Node, InitialNode> toNodes) {
		Set<Node> endNodes = new HashSet<>(toNodes.keySet());
		double minCost = Double.POSITIVE_INFINITY;

		// do the real work
		while (endNodes.size() > 0) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				// seems we have no more nodes left, but not yet reached all endNodes...
				endNodes.clear();
			} else {
				DijkstraNodeData data = getData(outNode);
				boolean isEndNode = endNodes.remove(outNode);
				if (isEndNode) {
					InitialNode initData = toNodes.get(outNode);
					double cost = data.getCost() + initData.initialCost;
					if (cost < minCost) {
						minCost = cost;
					}
				}
				if (data.getCost() > minCost) {
					endNodes.clear(); // we can't get any better now
				} else {
					relaxNode(outNode, pendingNodes);
				}
			}
		}
	}

	/**
	 * Method to request the passenger route from the (cached) fromNodes to the passed toNodes.
	 * Should only be requested after calling createTransitLeastCostPathTree().
	 *
	 * @param toNodes
	 *          The nodes that are the next stops to the toCoord and you like to route to.
	 *
	 * @return
	 *          the transitPassengerRoute between the fromNode and the toNode.
	 *          Will be null if the route could not be found.
	 */
	public TransitPassengerRoute getTransitPassengerRoute(final Map<Node, InitialNode> toNodes) {
		//find the best node
		double minCost = Double.POSITIVE_INFINITY;
		Node minCostNode = null;
		for (Map.Entry<Node,InitialNode> e : toNodes.entrySet()) {
			Node currentNode = e.getKey();
			DijkstraNodeData r = this.nodeData.get(currentNode.getId());
			if (r == null) {
				expandNodeData(toNodes);
			}
			DijkstraNodeData data = getData(currentNode);
			InitialNode initData = e.getValue();
			double cost = data.getCost() + initData.initialCost;
			if (data.getCost() != 0.0 || fromNodes.containsKey(currentNode)) {
				if (cost < minCost) {
					minCost = cost;
					minCostNode = currentNode;
				}
			}
		}

		if (minCostNode == null) {
			return null;
		}

		// now construct route segments, which are required for TransitPassengerRoute
		List<RouteSegment> routeSegments = new ArrayList<>();

		TransitRouterNetworkLink link = (TransitRouterNetworkLink) getData(minCostNode).getPrevLink();
		TransitRouterNetworkLink downstreamLink = null;
		Node previousFromNode = minCostNode;
		double transferCost = 0.;

		while (link != null) {
			TransitRouterNetworkNode fromNode = link.fromNode;
			TransitRouterNetworkNode toNode = link.toNode;

			double travelTime = getData(toNode).getTime() - getData(fromNode).getTime();
			Id<TransitLine> transitLineId = null;
			Id<TransitRoute> routeId = null;

			boolean isTransferLeg = false;
			if (link.line==null) isTransferLeg = true;
			else {
				transitLineId = link.line.getId();
				routeId = link.route.getId();
			}

			if (downstreamLink==null && isTransferLeg) {
				// continuous transfers: see TransitRouterImplTest.testDoubleWalk.
				// Another possibility is that trip start with transfer itself, see TransitRouterImplTest.testDoubleWalkOnly.
				double tempTravelTime = 0.;
				TransitStopFacility toStop = null;

				if (routeSegments.size()==0) { // very first leg starts with transfer
					toStop = toNode.stop.getStopFacility();
				} else {
					RouteSegment routeSegment = routeSegments.remove(0);
					travelTime = routeSegment.travelTime;
					toStop = routeSegment.toStop;
				}

				routeSegments.add(0,
						new RouteSegment(fromNode.stop.getStopFacility(),
						toStop,
						tempTravelTime+travelTime,
						transitLineId,
						routeId));
				//not sure, if transferCost will be included for every transfer or not. Currently, transfer cost will be accumulated for every transfer. Amit Sep'17
			} else if (downstreamLink == null // very first pt leg or first pt leg after transfer
					|| isTransferLeg ) {
				routeSegments.add(0, new RouteSegment( fromNode.stop.getStopFacility(),
						toNode.stop.getStopFacility(),
						travelTime,
						transitLineId,
						routeId
				));
			} else if (downstreamLink.line.getId() == link.line.getId() && downstreamLink.route.getId() == link.route.getId() ){
				//same route --> update the top routeSegment
				RouteSegment routeSegment = routeSegments.remove(0);
				routeSegments.add(0, new RouteSegment(fromNode.stop.getStopFacility(),
						routeSegment.toStop,
						routeSegment.travelTime+travelTime,
						transitLineId,
						routeId));
			}

			if (isTransferLeg) {
				// transfer cost
				if ( ! (this.costFunction instanceof  TransitRouterNetworkTravelTimeAndDisutility) ) {
					throw new RuntimeException("TransitTravelDisutility is not instance of "+TransitRouterNetworkTravelTimeAndDisutility.class.getSimpleName()
					+". An acc ");
				}

				transferCost += ((TransitRouterNetworkTravelTimeAndDisutility) this.costFunction).defaultTransferCost(link,
						Time.getUndefinedTime(),null,null);

				downstreamLink = null;
			} else {
				downstreamLink = link;
			}

			previousFromNode = fromNode;
			link = (TransitRouterNetworkLink) getData(fromNode).getPrevLink();
		}

		DijkstraNodeData startNodeData = getData(previousFromNode);
		DijkstraNodeData toNodeData = getData(minCostNode);

		double cost = toNodeData.getCost() - startNodeData.getCost()
				+ this.fromNodes.get(previousFromNode).initialCost
				+ toNodes.get(minCostNode).initialCost
				+ transferCost;

		// if there is no connection found, getPath(...) return a path with nothing in it, however, I (and AN) think that it should throw null. Amit Sep'17
		if (routeSegments.size()==0) return null;
		else return new TransitPassengerRoute(cost, routeSegments);
	}

	/**
	 * Method to request the path from the (cached) fromNodes to the passed toNodes.
	 * Should only be requested after calling createTransitLeastCostPathTree().
	 *
	 * @param toNodes
	 *          The nodes that are the next stops to the toCoord and you like to route to.
	 *
	 * @return
	 *          the leastCostPath between the fromNode and the toNode.
	 *          Will be null if the path could not be found.
	 */
	public Path getPath(final Map<Node, InitialNode> toNodes) {

		//find the best node
		double minCost = Double.POSITIVE_INFINITY;
		Node minCostNode = null;
		for (Map.Entry<Node, InitialNode> e : toNodes.entrySet()) {
			Node currentNode = e.getKey();
			DijkstraNodeData r = this.nodeData.get(currentNode.getId());
			if (r == null) {
				expandNodeData(toNodes);
			}
			DijkstraNodeData data = getData(currentNode);
			InitialNode initData = e.getValue();
			double cost = data.getCost() + initData.initialCost;
			if (data.getCost() != 0.0 || fromNodes.containsKey(currentNode)) {
				if (cost < minCost) {
					minCost = cost;
					minCostNode = currentNode;
				}
			}
		}

		if (minCostNode == null) {
			return null;
		}

		// now construct the path
		List<Node> nodes = new LinkedList<>();
		List<Link> links = new LinkedList<>();

		nodes.add(0, minCostNode);
		Link tmpLink = getData(minCostNode).getPrevLink();
		while (tmpLink != null) {
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
			tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
		}

		DijkstraNodeData startNodeData = getData(nodes.get(0));
		DijkstraNodeData toNodeData = getData(minCostNode);

		return new Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(),
				toNodeData.getCost() - startNodeData.getCost());
	}

	/**
	 * Allow replacing the RouterPriorityQueue.
	 */
	@SuppressWarnings("static-method")
	/*package*/ RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
		return new PseudoRemovePriorityQueue<>(500);
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
	 * @param outLink
	 *            The node from which we came visiting n.
	 */
	protected void visitNode(final Node n, final DijkstraNodeData data,
			final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Link outLink) {
		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(n, getPriority(data));
	}

	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *
	 * @param outNode
	 *            The Node to be expanded.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	protected void relaxNode(final Node outNode, final RouterPriorityQueue<Node> pendingNodes) {

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		for (Link l : outNode.getOutLinks().values()) {
			relaxNodeLogic(l, pendingNodes, currTime, currCost);
		}
	}

	/**
	 * Logic that was previously located in the relaxNode(...) method.
	 * By doing so, the FastDijkstra can overwrite relaxNode without copying the logic.
	 */
	/*package*/ void relaxNodeLogic(final Link l, final RouterPriorityQueue<Node> pendingNodes,
			final double currTime, final double currCost) {
		addToPendingNodes(l, l.getToNode(), pendingNodes, currTime, currCost);
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
	 * @return true if the node was added to the pending nodes, false otherwise
	 * 		(e.g. when the same node already has an earlier visiting time).
	 */
	protected boolean addToPendingNodes(final Link l, final Node n,
			final RouterPriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost) {

        this.customDataManager.initForLink(l);
        double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, this.person, this.vehicle);
        double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle, this.customDataManager);
        DijkstraNodeData data = getData(n);
        double nCost = data.getCost();
        if (!data.isVisited(getIterationId())) {
            visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);
            this.customDataManager.storeTmpData();
            return true;
        }
		double totalCost = currCost + travelCost;
        if (totalCost < nCost) {
            revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
            this.customDataManager.storeTmpData();
            return true;
        }

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
	 * @param outLink
	 *            The link from which we came visiting n.
	 */
	void revisitNode(final Node n, final DijkstraNodeData data,
			final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Link outLink) {
		pendingNodes.remove(n);

		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(n, getPriority(data));
	}

	/**
	 * The value used to sort the pending nodes during routing.
	 * This implementation compares the total effective travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 */
	private double getPriority(final DijkstraNodeData data) {
		return data.getCost();
	}

	/**
	 * Returns the data for the given node. Creates a new NodeData if none exists
	 * yet.
	 *
	 * @param n
	 *            The Node for which to return the data.
	 * @return The data for the given Node
	 */
	protected DijkstraNodeData getData(final Node n) {
		DijkstraNodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new DijkstraNodeData();
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}

}