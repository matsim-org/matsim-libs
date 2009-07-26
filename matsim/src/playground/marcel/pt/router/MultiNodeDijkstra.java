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

package playground.marcel.pt.router;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Network;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

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

	public Path calcLeastCostPath(final List<InitialNode> fromNodes, final List<InitialNode> toNodes) {
		
		Node toNode = toNodes.get(0).node; // just a random one
		Set<Node> startNodes = new HashSet<Node>();
		Set<Node> endNodes = new HashSet<Node>();
		Set<Node> foundNodes = new HashSet<Node>();
		for (InitialNode node : fromNodes) {
			startNodes.add(node.node);
		}
		for (InitialNode node : toNodes) {
			endNodes.add(node.node);
		}
		
		augmentIterationId();

		PriorityQueue<Node> pendingNodes = new PriorityQueue<Node>(500, this.comparator);
		for (InitialNode node : fromNodes) {
			DijkstraNodeData data = getData(node.node);
			visitNode(node.node, data, pendingNodes, node.initialTime, node.initialCost, null);
		}

		while (endNodes.size() > 0) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				log.warn("No route was found");
				return null;
			}

			if (endNodes.contains(outNode)) {
				endNodes.remove(outNode);
				foundNodes.add(outNode);
			}
			relaxNode(outNode, toNode, pendingNodes);
		}

		// find out which one is the cheapest end node
		double minCost = Double.POSITIVE_INFINITY;
		double arrivalTime = Double.NaN;
		Node minCostNode = null;
		for (InitialNode node : toNodes) {
			if (foundNodes.contains(node.node)) {
				DijkstraNodeData data = getData(node.node);
				double cost = data.getCost() + node.initialCost;
				if (cost < minCost) {
					arrivalTime = data.getTime() + node.initialTime;
					minCost = cost;
					minCostNode = node.node;
				}
			}
		}
		if (minCostNode == null) {
			log.warn("No route was found");
			return null;
		}
		toNode = minCostNode;

		// now construct the path
		List<Node> nodes = new LinkedList<Node>();
		List<Link> links = new LinkedList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink != null) {
				links.add(0, tmpLink);
				nodes.add(0, tmpLink.getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
		}
		double startTime = getData(nodes.get(0)).getTime();
		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		return path;
	}

	public static class InitialNode {
		public final Node node;
		public final double initialCost;
		public final double initialTime;
		public InitialNode(final Node node, final double initialCost, final double initialTime) {
			this.node = node;
			this.initialCost = initialCost;
			this.initialTime = initialTime;
		}
	}
}
