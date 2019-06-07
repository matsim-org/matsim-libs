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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.NodeDataFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.utils.collections.RouterPriorityQueue;

import java.util.ArrayList;

/*package*/ abstract class AbstractFastRouterDelegate implements FastRouterDelegate {

	/*package*/ final Dijkstra dijkstra;
	/*package*/ final NodeDataFactory nodeDataFactory;
	
	/*package*/ AbstractFastRouterDelegate(final Dijkstra dijkstra, final NodeDataFactory nodeDataFactory) {
		this.dijkstra = dijkstra;
		this.nodeDataFactory = nodeDataFactory;
	}
	
	@Override
	public void initialize() {
		// Some classes might override this method and do some additional stuff...
	}
	
	@Override
	public Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		ArrayList<Node> nodes = new ArrayList<>();
		ArrayList<Link> links = new ArrayList<>();

		nodes.add(0, ((RoutingNetworkNode) toNode).getNode());
		Link tmpLink = getData(toNode).getPrevLink();
//		if (tmpLink != null) {
			// original code
//			while (tmpLink.getFromNode() != fromNode) {
//				links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
//				nodes.add(0, ((RoutingNetworkLink) tmpLink).getLink().getFromNode());
//				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
//			}
//			links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
//			nodes.add(0, ((RoutingNetworkNode) tmpLink.getFromNode()).getNode());

			/*
			 * Adapted this code to be compatible with the MultiNodeDijkstra located in
			 * the location choice contrib. When a MultiNodeDijkstra uses multiple start nodes,
			 * there is not a single start node that could be used to check whether
			 * "tmpLink.getFromNode() != fromNode" is true. Instead, the start nodes do not have
			 * a previous link.
			 * For the regular Dikstra, this is also fine since the start node also does not have
			 * a previous node.
			 * cdobler, feb'14
			 */
			while (tmpLink != null) {
				links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
				nodes.add(0, ((RoutingNetworkLink) tmpLink).getLink().getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
//		}
		
		NodeData toNodeData = getData(toNode);
		return new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());
	}
	
	@Override
	public void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {

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

	@Override
	public PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return ((RoutingNetworkNode) n).getDeadEndData();
	}
}
