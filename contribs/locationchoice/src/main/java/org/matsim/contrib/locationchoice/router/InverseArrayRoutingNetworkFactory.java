/* *********************************************************************** *
 * project: org.matsim.*
 * InverseArrayRoutingNetworkFactory.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * For all links, their from and to nodes are exchanged. As a result,
 * the router can route backwards, i.e. from the destination to the target.
 * 
 * @author cdobler
 */
public class InverseArrayRoutingNetworkFactory extends AbstractRoutingNetworkFactory {
	
	private final static Logger log = Logger.getLogger(ArrayRoutingNetworkFactory.class);
	
	private int nodeArrayIndexCounter;
	private int linkArrayIndexCounter;
	
	public InverseArrayRoutingNetworkFactory(PreProcessDijkstra preProcessData) {
		super(preProcessData);
	}
	
	@Override
	public ArrayRoutingNetwork createRoutingNetwork(Network network) {
		this.nodeArrayIndexCounter = 0;
		this.linkArrayIndexCounter = 0;
		
		ArrayRoutingNetwork routingNetwork = new ArrayRoutingNetwork(network);
		
		for (Node node : network.getNodes().values()) {
			RoutingNetworkNode routingNode = createRoutingNetworkNode(node, node.getInLinks().size());
			routingNetwork.addNode(routingNode);
		}
		Map<Id, RoutingNetworkLink> routingLinks = new HashMap<>();
		for (Link link : network.getLinks().values()) {
			
			// switch from and to nodes here
			RoutingNetworkNode fromNode = routingNetwork.getNodes().get(link.getToNode().getId());
			RoutingNetworkNode toNode = routingNetwork.getNodes().get(link.getFromNode().getId());
			
			RoutingNetworkLink dijkstraLink = createRoutingNetworkLink(link, fromNode, toNode);
			routingLinks.put(dijkstraLink.getId(), dijkstraLink);
		}
		
		for (Node node : network.getNodes().values()) {
			RoutingNetworkLink[] outLinks = new RoutingNetworkLink[node.getInLinks().size()];
			
			int i = 0;
			for (Link inLink : node.getInLinks().values()) {
				outLinks[i] = routingLinks.remove(inLink.getId());
				i++;
			}
			
			RoutingNetworkNode dijkstraNode = routingNetwork.getNodes().get(node.getId());
			dijkstraNode.setOutLinksArray(outLinks);
		}
		
		if (routingLinks.size() > 0) log.warn("Not all links have been use in the ArrayRoutingNetwork - " +
				"check connectivity of input network!");
		
		if (preProcessData != null) {
			if (preProcessData.containsData()) {
				for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
					node.setDeadEndData(preProcessData.getNodeData(node.getNode()));
				}
			}
		}
		
		return routingNetwork;
	}

	@Override
	public ArrayRoutingNetworkNode createRoutingNetworkNode(Node node, int numOutLinks) {
		return new ArrayRoutingNetworkNode(node, numOutLinks, nodeArrayIndexCounter++);
	}

	@Override
	public ArrayRoutingNetworkLink createRoutingNetworkLink(Link link,
			RoutingNetworkNode fromNode, RoutingNetworkNode toNode) {
		return new ArrayRoutingNetworkLink(link, fromNode, toNode, linkArrayIndexCounter++);
	}
}