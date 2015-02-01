/* *********************************************************************** *
 * project: org.matsim.*
 * BackwardFastMultiNodeDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.collections.RouterPriorityQueue;

import java.util.ArrayList;

/**
 * This implementation of a backwards routing Dijkstra is based on an
 * inverted network, i.e. a network were the links' from and to nodes
 * are switched.
 * 
 * @author cdobler
 */
public class BackwardFastMultiNodeDijkstra extends FastMultiNodeDijkstra {
	
	public BackwardFastMultiNodeDijkstra(final RoutingNetwork routingNetwork, final TravelDisutility costFunction,
			final TravelTime timeFunction, final PreProcessDijkstra preProcessData, 
			final FastRouterDelegateFactory fastRouterFactory, boolean searchAllEndNodes) {
		super(routingNetwork, costFunction, timeFunction, preProcessData, fastRouterFactory, searchAllEndNodes);
	}
	
	@Override
	protected boolean addToPendingNodes(final Link l, final Node n,
			final RouterPriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost, final Node toNode) {
		/*
		 * This part is exchanging the two commented out lines. Maybe this could be moved
		 * to a separate method in the main class?
		 */
		// bw: travel time has to be negative while costs are still positive
		// But we have a problem if current time is negative. Use previous day instead! 
		double travelTime;
		double travelCost;
		if (currTime < 0) {
			double timeMod = 24.0 * 3600.0 - Math.abs(currTime % (24.0 * 3600.0));
			travelTime = -1.0 * this.timeFunction.getLinkTravelTime(l, timeMod, getPerson(), getVehicle());
			travelCost = this.costFunction.getLinkTravelDisutility(l, timeMod, getPerson(), getVehicle());			
		} else {
			travelTime = -1.0 * this.timeFunction.getLinkTravelTime(l, currTime, getPerson(), getVehicle());
			travelCost = this.costFunction.getLinkTravelDisutility(l, currTime,  getPerson(), getVehicle());
		}	
//		double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, person, vehicle);
//		double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle);

		/*
		 * This is copied from the default Dijkstra implementation.
		 */
		DijkstraNodeData data = getData(n);
		double nCost = data.getCost();
		if (!data.isVisited(getIterationId())) {
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);
			return true;
		}
		double totalCost = currCost + travelCost;
		if (totalCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
			return true;
		}

		return false;
	}
	
	@Override
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		
		ArrayList<Node> nodes = new ArrayList<>();
		ArrayList<Link> links = new ArrayList<>();

		nodes.add(((RoutingNetworkNode) toNode).getNode());
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(((RoutingNetworkLink) tmpLink).getLink());
				nodes.add(((RoutingNetworkLink) tmpLink).getLink().getToNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(((RoutingNetworkLink) tmpLink).getLink());
			nodes.add(((RoutingNetworkNode) tmpLink.getFromNode()).getNode());
		}
		
		NodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, startTime - arrivalTime, toNodeData.getCost());

		return path;
		
//		Path path = super.constructPath(fromNode, toNode, startTime, arrivalTime);
//		if (path != null) {
//			// invert nodes, links and travel time
//			List<Link> links = new ArrayList<Link>();
//			for (int i = path.links.size() - 1; i >= 0; i--) links.add(path.links.get(i));
//			
//			List<Node> nodes = new ArrayList<Node>();
//			for (int i = path.nodes.size() - 1; i >= 0; i--) nodes.add(path.nodes.get(i));
//			
//			double travelTime = -path.travelTime;
//			
//			return new Path(nodes, links, travelTime, path.travelCost);
//		} else return null;
	}
	
//	@Override
//	public Path constructPath(Node fromNode, Node toNode, double startTime) {
//		if (toNode == null) return null;
//		else {
//			if (!(fromNode instanceof RoutingNetworkNode)) fromNode = this.routingNetwork.getNodes().get(fromNode.getId());
//			if (!(toNode instanceof RoutingNetworkNode)) toNode = this.routingNetwork.getNodes().get(toNode.getId());
//
//			DijkstraNodeData outData = getData(toNode);
//			double arrivalTime = outData.getTime();
//			
//			// now construct and return the path
//			return constructPath(fromNode, toNode, startTime, arrivalTime);			
//		}
//	}
	
}