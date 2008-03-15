/* *********************************************************************** *
 * project: org.matsim.*
 * RouterNet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.router;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;

import teach.multiagent07.net.CALink;
import teach.multiagent07.net.CANetwork;
import teach.multiagent07.net.CANode;

public class RouterNet extends CANetwork {

	private final static Logger log = Logger.getLogger(RouterNet.class);
	
	@Override
	public CALink newLink(String label) {
		RouterLink link = new RouterLink(label);
		return link;
	}

	@Override
	public CANode newNode(String label) {
		RouterNode node = new RouterNode(label);
		return node;
	}

	// calcs the route using a PriorityQueue to store the pending nodes
	public final List<RouterNode> calcCheapestRoute(RouterNode fromNode, RouterNode toNode, double starttime) {

		// first make sure the cost and visit flags are all reset
		Iterator nIter = this.getNodes().values().iterator();
		while (nIter.hasNext()) {
			RouterNode node = (RouterNode)nIter.next();
			node.resetVisited();
		}

		// now start the dijkstra-algorithm
		boolean stillSearching = true;

		RouterNode.CostComparator comparator = (RouterNode.CostComparator)RouterNode.getCostComparator();
		PriorityQueue<RouterNode> pendingNodes = new PriorityQueue<RouterNode>(100, comparator);
		pendingNodes.add(fromNode);
		fromNode.visit(null, 0, starttime);

		// loop over pendingNodes, always handling the first one.
		while (stillSearching) {
			RouterNode outNode = pendingNodes.poll();
			if (outNode == null) {
			log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId() + " .");
				return null;
			}

			if (outNode.getId() == toNode.getId()) {
				stillSearching = false;
				break;
			}

			double currTime = outNode.getTime();
			double currCost = outNode.getCost();
			for (BasicLinkI l : outNode.getOutLinks().values()) {
				RouterLink link = (RouterLink) l;
				RouterNode node = (RouterNode)link.getToNode();
				double travelTime = link.getTravelTime(currTime);
				double travelCost = link.getLength() / link.getFreespeed();

				double nCost = node.getCost();
				if (!node.isVisited()) {
					node.visit(outNode, currCost + travelCost, currTime + travelTime);
					pendingNodes.add(node);
				} else if (currCost + travelCost < nCost) {
					// remove the old entry of node n
					// there must be one in the queue, as the visited flag is set!

					Iterator<RouterNode> iter2 = pendingNodes.iterator();
					while (iter2.hasNext() && iter2.next() != node) ;
					iter2.remove();

					node.visit(outNode, currCost + travelCost, currTime + travelTime);

					pendingNodes.add(node);
				}
			}
		}

		// now construct the route
		List<RouterNode> route = new ArrayList<RouterNode>();
		RouterNode tmpNode = toNode;
		while (tmpNode != fromNode) {
			route.add(0, tmpNode);
			tmpNode = tmpNode.getPrevNode();
		}
		route.add(0, tmpNode);	// add the fromNode at the beginning of the list

		return route;
	}
}
