/* *********************************************************************** *
 * project: org.matsim.*
 * FastRouterDelegate.java
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
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.utils.collections.RouterPriorityQueue;

/**
 * This class is used by the faster implementations of the Dijkstra, AStarEuclidean and
 * AStarLandmarks router. Basically, the methods perform the conversation from the
 * Network to the RoutingNetwork where the routing data is stored in the nodes and not
 * in maps.
 * 
 * @author cdobler
 */
/*package*/ interface FastRouterDelegate {
	
	/*
	 * Some implementations might use this for lazy initialization.
	 */
	/*package*/ void initialize();
	
	/*
	 * Constructs the path and replaces the nodes and links from the routing network
	 * with their corresponding nodes and links from the network.
	 */
	/*package*/ Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime);	
	/*
	 * For performance reasons the outgoing links of a node are stored in
	 * the routing network in an array instead of a map. Therefore we have
	 * to iterate over an array instead of over a map. 
	 */
	/*package*/ void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes);	

	/*
	 * The NodeData is taken from the RoutingNetworkNode and not from a map.
	 */
	/*package*/ NodeData getData(final Node n);

	/*
	 * The DeadEndData is taken from the RoutingNetworkNode and not from a map.
	 */
	/*package*/ PreProcessDijkstra.DeadEndData getPreProcessData(final Node n);
}
