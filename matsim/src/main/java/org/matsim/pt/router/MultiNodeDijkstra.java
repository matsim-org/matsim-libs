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

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;

public class MultiNodeDijkstra /*extends Dijkstra*/ {

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
	
	public MultiNodeDijkstra(final Network network, final TransitTravelDisutility costFunction, final TravelTime timeFunction) {
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
	}

	@SuppressWarnings("unchecked")
	public Path calcLeastCostPath(final Map<Node, InitialNode> fromNodes, final Map<Node, InitialNode> toNodes, final Person person) {
		Map<Node, InitialNode> swapedToNodes = swapNodes(toNodes);
		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, costFunction, timeFunction, swapNodes(fromNodes), swapedToNodes, person);
		return tree.getPath(swapedToNodes);
	}

	private Map<Node, InitialNode> swapNodes(final Map<Node, InitialNode> original) {
		Map<Node, InitialNode> result = new HashMap<>();
		for (Map.Entry<Node, InitialNode> entry : original.entrySet()) {
			result.put(entry.getKey(), new InitialNode(entry.getValue().initialCost, entry.getValue().initialTime));
		}
		return result;
	}

}
