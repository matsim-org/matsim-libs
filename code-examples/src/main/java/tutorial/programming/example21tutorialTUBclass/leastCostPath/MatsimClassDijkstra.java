/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package tutorial.programming.example21tutorialTUBclass.leastCostPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author jbischoff
 *this is a very simple Dijkstra developed in the Matsim class @ TUB
 * 	
//
 */
public final class MatsimClassDijkstra implements LeastCostPathCalculator {

	private final Network network;
	private Map<Id<Node>,Double> costToNode = new HashMap<Id<Node>, Double>();
	private Map<Id<Node>,Id<Node>> previousNodes = new HashMap<Id<Node>, Id<Node>>();
	PriorityQueue<Id<Node>> queue = new PriorityQueue<Id<Node>>(11, new Comparator<Id<Node>>() {

		@Override
		public int compare(Id<Node> o1, Id<Node> o2) {
			return costToNode.get(o1).compareTo(costToNode.get(o2));
		}

	});
	MatsimClassDijkstra(Network network, TravelDisutility travelCosts,
			TravelTime travelTimes) {
		this.network = network;

	}

	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
			Person person, Vehicle vehicle) {

		initializeNetwork(fromNode.getId());

		while (!queue.isEmpty()) {
			Id<Node> currentId = queue.poll();
			if (currentId == toNode.getId()) return createPath(toNode.getId(),fromNode.getId());
			Node currentNode = network.getNodes().get(currentId);
			for (Link link:  currentNode.getOutLinks().values()){
				Node currentToNode = link.getToNode();
				double distance = link.getLength() + this.costToNode.get(currentId);
				if (distance < this.costToNode.get(currentToNode.getId())){
					this.costToNode.put(currentToNode.getId(), distance);
					update(currentToNode.getId());
					this.previousNodes.put(currentToNode.getId(), currentId);
				}
			}
		}

		return null;
	}

	private Path createPath(Id<Node> toNodeId, Id<Node> fromNodeId) {
		List<Node> nodes = new ArrayList<Node>();
		List<Link> links = new ArrayList<Link>();
		Node lastNode = network.getNodes().get(toNodeId);
		while (!lastNode.getId().equals(fromNodeId)){
			if (!lastNode.getId().equals(toNodeId)) 
				nodes.add(0, lastNode);
			Node newLastNode = network.getNodes().get(this.previousNodes.get(lastNode.getId()));
			Link l = NetworkUtils.getConnectingLink(newLastNode,lastNode);
			links.add(0, l);
			lastNode = newLastNode;
		}


		return new Path(nodes,links,0.0,0.0);
	}

	private void initializeNetwork(Id<Node> startNode) {
		for (Node node : network.getNodes().values()){
			this.costToNode.put(node.getId(), Double.POSITIVE_INFINITY);
			this.previousNodes.put(node.getId(), null);
		}
		this.costToNode.put(startNode, 0.0);
		this.queue.add(startNode);

	}
	private void update(Id<Node> nodeToUpdate){
		this.queue.remove(nodeToUpdate);
		this.queue.add(nodeToUpdate);
	}

}
