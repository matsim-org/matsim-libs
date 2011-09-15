/* *********************************************************************** *
 * project: org.matsim.*
 * FastRoutingNetworkFactory.java
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

package org.matsim.core.router.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

public class RoutingNetworkFactory implements NetworkFactory {
	
	private final static Logger log = Logger.getLogger(RoutingNetworkFactory.class);
	
	public RoutingNetwork createDijkstraNetwork(Network network) {
		
		RoutingNetwork dijkstraNetwork = new RoutingNetwork(network);
		
		for (Node node : network.getNodes().values()) {
			RoutingNetworkNode dijkstraNode = new RoutingNetworkNode(node);
			dijkstraNetwork.addNode(dijkstraNode);
		}
		
		Map<Id, RoutingNetworkLink> dijkstraLinks = new HashMap<Id, RoutingNetworkLink>();
		for (Link link : network.getLinks().values()) {
			RoutingNetworkNode fromNode = dijkstraNetwork.getNodes().get(link.getFromNode().getId());
			RoutingNetworkNode toNode = dijkstraNetwork.getNodes().get(link.getToNode().getId());
			RoutingNetworkLink dijkstraLink = new RoutingNetworkLink(link, fromNode, toNode);
			dijkstraLinks.put(dijkstraLink.getId(), dijkstraLink);
		}
		
		for (Node node : network.getNodes().values()) {
			
			RoutingNetworkLink[] outLinks = new RoutingNetworkLink[node.getOutLinks().size()];
			
			int i = 0;
			for (Link outLink : node.getOutLinks().values()) {
				
				outLinks[i] = dijkstraLinks.remove(outLink.getId());
				i++;
			}
			
			RoutingNetworkNode dijkstraNode = dijkstraNetwork.getNodes().get(node.getId());
			dijkstraNode.setOutLinksArray(outLinks);
		}
		
		if (dijkstraLinks.size() > 0) log.warn("Not all links have been use in the DijkstraNetwork - check connectivity of input network!");
		dijkstraLinks.clear();
		dijkstraLinks = null;
		
		return dijkstraNetwork;
	}

	@Override
	public Link createLink(Id id, Id fromNodeId, Id toNodeId) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Link createLink(Id id, Node fromNode, Node toNode) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public RoutingNetworkNode createNode(Id id, Coord coord) {
		throw new RuntimeException("Not supported operation!");
	}

}
