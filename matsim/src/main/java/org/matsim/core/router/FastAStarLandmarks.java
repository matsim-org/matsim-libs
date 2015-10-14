/* *********************************************************************** *
 * project: org.matsim.*
 * FastAStarLandmarks.java
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
import org.matsim.core.router.util.AStarNodeData;
import org.matsim.core.router.util.AStarNodeDataFactory;
import org.matsim.core.router.util.ArrayRoutingNetwork;
import org.matsim.core.router.util.ArrayRoutingNetworkNode;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * <p>
 * Performance optimized version of the Dijkstra {@link org.matsim.core.router.FastAStarLandmarks} 
 * least cost path router which uses its own network to route within.
 * </p>
 * 
 * @see org.matsim.core.router.FastAStarLandmarks
 * @see org.matsim.core.router.util.RoutingNetwork
 * @author cdobler
 */
public class FastAStarLandmarks extends AStarLandmarks {

	private final RoutingNetwork routingNetwork;
	private final FastRouterDelegate fastRouter;
	private BinaryMinHeap<ArrayRoutingNetworkNode> heap = null;
	private int maxSize = -1;

	public FastAStarLandmarks(final RoutingNetwork routingNetwork, final PreProcessLandmarks preProcessData,
			final TravelDisutility costFunction, final TravelTime timeFunction, final double overdoFactor,
			final FastRouterDelegateFactory fastRouterFactory) {
		super(routingNetwork, preProcessData, costFunction, timeFunction, overdoFactor);

		this.routingNetwork = routingNetwork;
		this.fastRouter = fastRouterFactory.createFastRouterDelegate(this, new AStarNodeDataFactory(), routingNetwork);
				
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
		
		RoutingNetworkNode routingNetworkFromNode = routingNetwork.getNodes().get(fromNode.getId());
		RoutingNetworkNode routingNetworkToNode = routingNetwork.getNodes().get(toNode.getId());

		if (this.landmarks.length >= 2) {
			initializeActiveLandmarks(routingNetworkFromNode, routingNetworkToNode, 2);
		} else {
			initializeActiveLandmarks(routingNetworkFromNode, routingNetworkToNode, this.landmarks.length);
		}
		
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
		return fastRouter.constructPath(fromNode, toNode, startTime, arrivalTime);
	}
	
	/*
	 * For performance reasons the outgoing links of a node are stored in
	 * the routing network in an array instead of a map. Therefore we have
	 * to iterate over an array instead of over a map. 
	 */
	@Override
	protected void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		this.controlCounter++;
		if (this.controlCounter == controlInterval) {
			int newLandmarkIndex = checkToAddLandmark(outNode, toNode);
			if (newLandmarkIndex > 0) {
				updatePendingNodes(newLandmarkIndex, toNode, pendingNodes);
			}
			this.controlCounter = 0;
		}
		
		fastRouter.relaxNode(outNode, toNode, pendingNodes);
	}
	
	/*
	 * The DijkstraNodeData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected AStarNodeData getData(final Node n) {
		return (AStarNodeData) fastRouter.getData(n);
	}

	/*
	 * The LandmarksData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected PreProcessLandmarks.LandmarksData getPreProcessData(final Node n) {
		return (PreProcessLandmarks.LandmarksData) fastRouter.getPreProcessData(n);
	}
}
