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

package playground.mrieser.pt.router;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;


/**
 * A variant of Dijkstra's algorithm for route finding that supports multiple
 * nodes as start and end. Each start/end node can contain a specific cost
 * component that describes the cost to reach that node to find the least cost
 * path to some place not part of the network.
 *
 * @author mrieser
 */
public class MultiNodeDijkstra extends Dijkstra {

	private static final Logger log = Logger.getLogger(MultiNodeDijkstra.class);

	public MultiNodeDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	public MultiNodeDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {
		super(network, costFunction, timeFunction, preProcessData);
	}

	public Path calcLeastCostPath(final Map<Node, InitialNode> fromNodes, final Map<Node, InitialNode> toNodes) {

		Set<Node> endNodes = new HashSet<Node>(toNodes.keySet());
		Set<Node> foundNodes = new HashSet<Node>();

		augmentIterationId();

		PseudoRemovePriorityQueue<Node> pendingNodes = new PseudoRemovePriorityQueue<Node>(500);
		for (Map.Entry<Node, InitialNode> entry : fromNodes.entrySet()) {
			DijkstraNodeData data = getData(entry.getKey());
			visitNode(entry.getKey(), data, pendingNodes, entry.getValue().initialTime, entry.getValue().initialCost, null);
		}

		// find out which one is the cheapest end node
		double minCost = Double.POSITIVE_INFINITY;
		Node minCostNode = null;

		// do the real work
		while (endNodes.size() > 0) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				// seems we have no more nodes left, but not yet reached all endNodes...
				endNodes.clear();
			} else {
				if (endNodes.contains(outNode)) {
					endNodes.remove(outNode);
					foundNodes.add(outNode);
					DijkstraNodeData data = getData(outNode);
					InitialNode initData = toNodes.get(outNode);
					double cost = data.getCost() + initData.initialCost;
					if (cost < minCost) {
						minCost = cost;
						minCostNode = outNode;
					}
				}
				DijkstraNodeData data = getData(outNode);
				if (data.getCost() > minCost) {
					endNodes.clear(); // we can't get any better now
				} else {
					relaxNode(outNode, null, pendingNodes);
				}
			}
		}

		if (minCostNode == null) {
			log.warn("No route was found");
			return null;
		}
		Node toNode = minCostNode;

		// now construct the path
		List<Node> nodes = new LinkedList<Node>();
		List<Link> links = new LinkedList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		while (tmpLink != null) {
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
			tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
		}
		DijkstraNodeData startNodeData = getData(nodes.get(0));
		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(), toNodeData.getCost() - startNodeData.getCost());

		return path;
	}

	public static class InitialNode {
		public final double initialCost;
		public final double initialTime;
		public InitialNode(final Node node, final double initialCost, final double initialTime) {
			this.initialCost = initialCost;
			this.initialTime = initialTime;
		}
	}
}
