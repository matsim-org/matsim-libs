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

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.NodeDataFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;

/**
 * This class is used by the faster implementations of the Dijkstra, AStarEuclidean and
 * AStarLandmarks router. Basically, the methods perform the conversation from the
 * Network to the RoutingNetwork where the routing data is stored in the nodes and not
 * in maps.
 * 
 * @author cdobler
 */
public class FastRouterDelegate {

	private final Dijkstra dijkstra;
	private final NodeDataFactory nodeDataFactory;
	
	/*package*/ FastRouterDelegate(final Dijkstra dijkstra, final NodeDataFactory nodeDataFactory) {
		this.dijkstra = dijkstra;
		this.nodeDataFactory = nodeDataFactory;
	}
	
	/*
	 * Constructs the path and replaces the nodes and links from the routing network
	 * with their corresponding nodes and links from the network.
	 */
	/*package*/ Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, ((RoutingNetworkNode) toNode).getNode());
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
				nodes.add(0, ((RoutingNetworkLink) tmpLink).getLink().getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
			nodes.add(0, ((RoutingNetworkNode) tmpLink.getFromNode()).getNode());
		}
		
		NodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		return path;
	}
	
	/*
	 * For performance reasons the outgoing links of a node are stored in
	 * the routing network in an array instead of a map. Therefore we have
	 * to iterate over an array instead of over a map. 
	 */
	/*package*/ void relaxNode(final Node outNode, final Node toNode, final PseudoRemovePriorityQueue<Node> pendingNodes) {

		RoutingNetworkNode routingNetworkNode = (RoutingNetworkNode) outNode;
		NodeData outData = getData(routingNetworkNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		if (this.dijkstra.pruneDeadEnds) {
			PreProcessDijkstra.DeadEndData ddOutData = getPreProcessData(routingNetworkNode);

			for (Link l : routingNetworkNode.getOutLinksArray()) {
				this.dijkstra.relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, ddOutData);
			}
		} else { // this.pruneDeadEnds == false
			for (Link l : routingNetworkNode.getOutLinksArray()) {
				this.dijkstra.relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, null);
			}				
		}
	}
	
	/*
	 * The NodeData is taken from the RoutingNetworkNode and not from a map.
	 */
	/*package*/ NodeData getData(final Node n) {
		RoutingNetworkNode routingNetworkNode = (RoutingNetworkNode) n;
		NodeData data;
		data = routingNetworkNode.getNodeData();
		
		if (data == null) {
			data = nodeDataFactory.createNodeData();
			routingNetworkNode.setNodeData(data);
		}
		return data;
	}

	/*
	 * The DeadEndData is taken from the RoutingNetworkNode and not from a map.
	 */
	/*package*/ PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return ((RoutingNetworkNode) n).getDeadEndData();
	}
}
