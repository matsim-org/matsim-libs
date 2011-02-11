/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
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

package org.matsim.locationchoice.bestresponse;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;


public class BackwardDijkstraMultipleDestinations extends Dijkstra {

	private final static Logger log = Logger.getLogger(BackwardDijkstraMultipleDestinations.class);
	final TravelCost costFunction;
	final TravelTime timeFunction;
	private int iterationID = Integer.MIN_VALUE + 1;
	private Node deadEndEntryNode;
	private final boolean pruneDeadEnds;

	public BackwardDijkstraMultipleDestinations(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
		this(network, costFunction, timeFunction, null);
	}

	public BackwardDijkstraMultipleDestinations(final Network network, final TravelCost costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {
		
		super(network, costFunction, timeFunction, preProcessData);

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		if (preProcessData != null) {
			if (preProcessData.containsData() == false) {
				this.pruneDeadEnds = false;
				log.warn("The preprocessing data provided to router class Dijkstra contains no data! Please execute its run(...) method first!");
				log.warn("Running without dead-end pruning.");
			} else {
				this.pruneDeadEnds = true;
			}
		} else {
			this.pruneDeadEnds = false;
		}
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {

		double arrivalTime = 0;
		augmentIterationId();
		
		if (this.pruneDeadEnds == true) {
			this.deadEndEntryNode = getPreProcessData(toNode).getDeadEndEntryNode();
		}
		
		// now construct the path, traversing backwards 
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			//bw: from Node is ok, we only need to change the from and to link below
			while (tmpLink.getFromNode() != fromNode) {
				links.add(0, tmpLink);
				nodes.add(0, tmpLink.getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
		}
		DijkstraNodeData toNodeData = getData(toNode);
		arrivalTime = toNodeData.getTime();
		// bw: -1.0 * times as we are going backwards (startTime > arrivalTime)
		Path path = new Path(nodes, links, -1.0 * (arrivalTime - startTime), toNodeData.getCost());

		return path;
	}
	
	public void calcLeastCostTree(Node fromNode, double startTime) {

		augmentIterationId();

		PseudoRemovePriorityQueue<Node> pendingNodes = new PseudoRemovePriorityQueue<Node>(500);
		// initFromNode
		DijkstraNodeData data = getData(fromNode);
		visitNode(fromNode, data, pendingNodes, startTime, 0, null);

		while (true) {
			Node outNode = pendingNodes.poll();
			if (outNode == null) return;
			relaxNode(outNode, null, pendingNodes);
		}
	}

	protected void relaxNode(final Node outNode, final Node toNode, final PseudoRemovePriorityQueue<Node> pendingNodes) {

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		if (this.pruneDeadEnds) {
			PreProcessDijkstra.DeadEndData ddOutData = getPreProcessData(outNode);
			// bw: changed from "getOutLinks" to "getInLinks"
			for (Link l : outNode.getInLinks().values()) {
				if (canPassLink(l)) {
					// bw: changed from "getToNode"
					Node n = l.getFromNode();
					PreProcessDijkstra.DeadEndData ddData = getPreProcessData(n);

					/* IF the current node n is not in a dead end
					 * OR it is in the same dead end as the fromNode
					 * OR it is in the same dead end as the toNode
					 * THEN we add the current node to the pending nodes */
					if ((ddData.getDeadEndEntryNode() == null)
							|| (ddOutData.getDeadEndEntryNode() != null)
							|| ((this.deadEndEntryNode != null)
									&& (this.deadEndEntryNode.getId() == ddData.getDeadEndEntryNode().getId()))) {
						addToPendingNodes(l, n, pendingNodes, currTime, currCost, toNode);
					}
				}
			}
		} else { // this.pruneDeadEnds == false
			// bw: changed from "getOutLinks" to "getInLinks"
			for (Link l : outNode.getInLinks().values()) {
				if (canPassLink(l)) {
					addToPendingNodes(l, l.getFromNode(), pendingNodes, currTime, currCost, toNode);
				}
			}
		}
	}

	protected boolean addToPendingNodes(final Link l, final Node n,
			final PseudoRemovePriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost, final Node toNode) {

		// bw: travel time has to be negative while costs are still positive
		double travelTime = -1.0 * this.timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = this.costFunction.getLinkGeneralizedTravelCost(l, currTime);
		DijkstraNodeData data = getData(n);
		double nCost = data.getCost();
		if (!data.isVisited(this.iterationID)) {
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, l);
			return true;
		}
		double totalCost = currCost + travelCost;
		if (totalCost < nCost) {
			//revisitNode:
			pendingNodes.remove(n);
			data.visit(l, totalCost, currTime + travelTime, this.iterationID);
			pendingNodes.add(n, getPriority(data));
			
			return true;
		}

		return false;
	}

	protected void visitNode(final Node n, final DijkstraNodeData data,
			final PseudoRemovePriorityQueue<Node> pendingNodes, final double time, final double cost,
			final Link outLink) {
		data.visit(outLink, cost, time, this.iterationID);
		pendingNodes.add(n, getPriority(data));
	}
}