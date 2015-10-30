/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;
import org.matsim.core.router.util.ArrayRoutingNetwork;
import org.matsim.core.router.util.ArrayRoutingNetworkNode;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.DijkstraNodeDataFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * <p>
 * Performance optimized version of the Dijkstra {@link org.matsim.core.router.Dijkstra} 
 * least cost path router which uses its own network to route within.
 * </p>
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.util.RoutingNetwork
 * @author cdobler
 */
public class FastDijkstra extends Dijkstra {

	private final RoutingNetwork routingNetwork;
	private final FastRouterDelegate fastRouter;
	private BinaryMinHeap<ArrayRoutingNetworkNode> heap = null;
	private int maxSize = -1;
	
	/*
	 * Create the routing network here and clear the nodeData map 
	 * which is not used by this implementation.
	 */
	public FastDijkstra(final RoutingNetwork routingNetwork, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData, final FastRouterDelegateFactory fastRouterFactory) {
		super(routingNetwork, costFunction, timeFunction, preProcessData);
		
		this.routingNetwork = routingNetwork;
		this.fastRouter = fastRouterFactory.createFastRouterDelegate(this, new DijkstraNodeDataFactory(), routingNetwork);

		this.nodeData.clear();
	}
		
	/*
	 * Replace the references to the from and to nodes with their corresponding
	 * nodes in the routing network.
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person, final Vehicle vehicle) {
		
		this.fastRouter.initialize();
		this.routingNetwork.initialize();
		
		RoutingNetworkNode routingNetworkFromNode = this.routingNetwork.getNodes().get(fromNode.getId());
		RoutingNetworkNode routingNetworkToNode = this.routingNetwork.getNodes().get(toNode.getId());

		return super.calcLeastCostPath(routingNetworkFromNode, routingNetworkToNode, startTime, person, vehicle);
	}
	
	@Override
	/*package*/ RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
		/*
		 * Re-use existing BinaryMinHeap instead of creating a new one. For large networks (> 10^6 nodes and links) this reduced
		 * the computation time by 40%! cdobler, oct'15
		 */
		if (this.routingNetwork instanceof ArrayRoutingNetwork) {
			int size = this.routingNetwork.getNodes().size();
			if (this.heap == null || this.maxSize != size) {
				this.maxSize = size;
				this.heap = new BinaryMinHeap<>(maxSize);
				return this.heap;
			} else {
				this.heap.reset();
				return this.heap;
			}
//			int maxSize = this.routingNetwork.getNodes().size();
//			return new BinaryMinHeap<ArrayRoutingNetworkNode>(maxSize);
		} else {
			return super.createRouterPriorityQueue();
		}
	}
	
	/*
	 * Constructs the path and replaces the nodes and links from the routing network
	 * with their corresponding nodes and links from the network.
	 */
	@Override
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		return this.fastRouter.constructPath(fromNode, toNode, startTime, arrivalTime);
	}
	
	/*
	 * For performance reasons the outgoing links of a node are stored in
	 * the routing network in an array instead of a map. Therefore we have
	 * to iterate over an array instead of over a map. 
	 */
	@Override
	protected void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		this.fastRouter.relaxNode(outNode, toNode, pendingNodes);
	}
	
	/*
	 * The DijkstraNodeData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected DijkstraNodeData getData(final Node n) {
		return (DijkstraNodeData) this.fastRouter.getData(n);
	}

	/*
	 * The DeadEndData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return this.fastRouter.getPreProcessData(n);
	}
}
