/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.zzunused;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;
import org.matsim.vehicles.Vehicle;

@Deprecated // (I think)
class ForwardDijkstraMultipleDestinations extends Dijkstra {
		
	public ForwardDijkstraMultipleDestinations(Network network, TravelDisutility costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person, final Vehicle vehicle) {
		// yyyyyy could you please explain why this needs to be re-implemented?  I would feel better if this would just use the method of
		// the original class.  kai, jan'13
		
		
		// now construct the path
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(0, tmpLink);
				nodes.add(0, tmpLink.getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
		}
		// arrival time was not ok in earlier versions!
		DijkstraNodeData toNodeData = getData(toNode);
		double arrivalTime = toNodeData.getTime();
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		return path;
	}
		
	public void calcLeastCostTree(Node fromNode, double startTime) {

		augmentIterationId();

		PseudoRemovePriorityQueue<Node> pendingNodes = new PseudoRemovePriorityQueue<Node>(500);
		
		//initFromNode
		DijkstraNodeData data = getData(fromNode);
		visitNode(fromNode, data, pendingNodes, startTime, 0, null);

		while (true) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) return;

			relaxNode(outNode, null, pendingNodes);
		}
	}
}
