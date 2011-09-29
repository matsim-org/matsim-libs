/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingNetwork.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

/**
 * A network that is used by FastDijkstra, FastAStarEuclidean and FastAStarLandmarks.
 * Instead of storing the node data in a map, the data is attached directly to the nodes
 * which is faster but also consumes more memory.
 * 
 * @see org.matsim.core.router.FastDijkstra
 * @see org.matsim.core.router.FastAStarEuclidean
 * @see org.matsim.core.router.FastAStarLandmarks
 * @author cdobler
 */
public class RoutingNetwork implements Network {
	
	private final static Logger log = Logger.getLogger(RoutingNetwork.class);
	
	private boolean isInitialized = false;
	private final Map<Id, RoutingNetworkNode> nodes = new HashMap<Id, RoutingNetworkNode>();
	private final Network network;
	private PreProcessDijkstra preProcessData;
	
	public RoutingNetwork(Network network) {
		this.network = network;
	}
	
	public void setPreProcessDijkstra(PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}
	
	public void initialize() {
		if (!isInitialized) {			
			for (Node node : network.getNodes().values()) {
				RoutingNetworkNode dijkstraNode = new RoutingNetworkNode(node);
				this.addNode(dijkstraNode);
			}
			
			Map<Id, RoutingNetworkLink> dijkstraLinks = new HashMap<Id, RoutingNetworkLink>();
			for (Link link : network.getLinks().values()) {
				RoutingNetworkNode fromNode = this.nodes.get(link.getFromNode().getId());
				RoutingNetworkNode toNode = this.nodes.get(link.getToNode().getId());
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
				
				RoutingNetworkNode dijkstraNode = this.nodes.get(node.getId());
				dijkstraNode.setOutLinksArray(outLinks);
			}
			
			if (dijkstraLinks.size() > 0) log.warn("Not all links have been use in the DijkstraNetwork - check connectivity of input network!");
			dijkstraLinks.clear();
			dijkstraLinks = null;
			
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
	
	@Override
	public NetworkFactory getFactory() {
		return network.getFactory();
	}

	@Override
	public Map<Id, RoutingNetworkNode> getNodes() {
		return nodes;
	}

	private void addNode(RoutingNetworkNode node) {
		this.nodes.put(node.getId(), node);
	}

	@Override
	public RoutingNetworkNode removeNode(Id nodeId) {
		return nodes.remove(nodeId);
	}

	@Override
	public void addLink(Link ll) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void addNode(Node nn) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getCapacityPeriod() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getEffectiveLaneWidth() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id, ? extends Link> getLinks() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Link removeLink(Id linkId) {
		throw new RuntimeException("Not supported operation!");
	}

}