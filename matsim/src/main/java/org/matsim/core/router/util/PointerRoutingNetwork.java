/* *********************************************************************** *
 * project: org.matsim.*
 * PointerRoutingNetwork.java
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

package org.matsim.core.router.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class PointerRoutingNetwork extends AbstractRoutingNetwork {
	
	private final static Logger log = Logger.getLogger(PointerRoutingNetwork.class);
	
	private final RoutingNetworkFactory networkFactory;
	private boolean isInitialized = false;
	
	public PointerRoutingNetwork(Network network, RoutingNetworkFactory networkFactory) {
		super(network);
		this.networkFactory = networkFactory;
	}
	
	/*package*/ void setPreProcessDijkstra(PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}
	
	@Override
	public void initialize() {
		if (!isInitialized) {			
			for (Node node : network.getNodes().values()) {
				RoutingNetworkNode routingNode = networkFactory.createRoutingNetworkNode(node, node.getOutLinks().size());
				this.addNode(routingNode);
			}
			
			Map<Id, RoutingNetworkLink> routingLinks = new HashMap<Id, RoutingNetworkLink>();
			for (Link link : network.getLinks().values()) {
				RoutingNetworkNode fromNode = this.nodes.get(link.getFromNode().getId());
				RoutingNetworkNode toNode = this.nodes.get(link.getToNode().getId());
				RoutingNetworkLink dijkstraLink = networkFactory.createRoutingNetworkLink(link, fromNode, toNode);
				routingLinks.put(dijkstraLink.getId(), dijkstraLink);
			}
			
			for (Node node : network.getNodes().values()) {
				RoutingNetworkLink[] outLinks = new RoutingNetworkLink[node.getOutLinks().size()];
				
				int i = 0;
				for (Link outLink : node.getOutLinks().values()) {
					outLinks[i] = routingLinks.remove(outLink.getId());
					i++;
				}
				
				RoutingNetworkNode dijkstraNode = this.nodes.get(node.getId());
				dijkstraNode.setOutLinksArray(outLinks);
			}
			
			if (routingLinks.size() > 0) log.warn("Not all links have been use in the PointerRoutingNetwork - check connectivity of input network!");
			
			if (preProcessData != null) {
				if (preProcessData.containsData()) {
					for (RoutingNetworkNode node : nodes.values()) {
						node.setDeadEndData(preProcessData.getNodeData(node.getNode()));				
					}
				}
			}
			preProcessData = null;
			
			isInitialized = true;
		}
	}
}