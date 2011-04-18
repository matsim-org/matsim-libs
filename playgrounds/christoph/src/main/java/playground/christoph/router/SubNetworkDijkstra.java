/* *********************************************************************** *
 * project: org.matsim.*
 * SubNetworkDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.network.SubLink;
import playground.christoph.network.SubNode;
import playground.christoph.router.util.KnowledgeTools;

/*
 * This is an extended version of the Dijkstra Least
 * Cost Path Algorithm.
 * The used Network can be changed which allows to use
 * different Networks for different Persons.
 * -> Every Person can route in its personalized SubNetwork!
 */
public class SubNetworkDijkstra extends Dijkstra {
	
	private Person person;
	private KnowledgeTools knowledgeTools = new KnowledgeTools();
	
	public SubNetworkDijkstra(Network network, TravelCost costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	public void setNetwork(Network network) {
		this.network = network;
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}
	
	/*
	 * The handed over fromNode and toNode are Nodes from the
	 * underlying, full Network. We have to replace them with
	 * their children in the Person's SubNetwork.
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {
		Network subNetwork = this.knowledgeTools.getSubNetwork(person, network);
		
		Node newFromNode = subNetwork.getNodes().get(fromNode.getId());
		Node newToNode = subNetwork.getNodes().get(toNode.getId());

//		if (this.network.getNodes().size() == 0) System.out.println("No Nodes in SubNetwork!");
//		
//		if (fromNode == null) System.out.println("FromNode Null!");
//		if (toNode == null) System.out.println("ToNode Null!");
//		
//		if (newFromNode == null) System.out.println("NewFromNode Null!");
//		else if (newFromNode.getId() == null) System.out.println("NewFromNodeId Null!");
//		
		if (newToNode == null) 	System.out.println("NewToNode Null!");
//		else if (newToNode.getId() == null) System.out.println("NewToNodeId Null!");
		
		Path path = super.calcLeastCostPath(newFromNode, newToNode, startTime);
		
		/*
		 *  The path contains SubNodes and SubLinks - we have to replace them with
		 *  their Parents.
		 */
		List<Node> newNodes = new ArrayList<Node>();
		for(Node node : path.nodes) {
			if (node instanceof SubNode) newNodes.add(((SubNode)node).getParentNode());
			else newNodes.add(node);
		}
		path.nodes.clear();
		path.nodes.addAll(newNodes);
		
		List<Link> newLinks = new ArrayList<Link>();
		for(Link link : path.links) {
			if (link instanceof SubLink) newLinks.add(((SubLink)link).getParentLink());
			else newLinks.add(link);
		}
		path.links.clear();
		path.links.addAll(newLinks);
		
		return path;
	}	
}