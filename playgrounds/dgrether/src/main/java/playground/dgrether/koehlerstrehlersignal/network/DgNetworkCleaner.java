/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
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

package playground.dgrether.koehlerstrehlersignal.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;

public class DgNetworkCleaner  {

	private static final Logger log = Logger.getLogger(DgNetworkCleaner.class);

	private Map<Id, Node> findBackwardCluster(final Node startNode, final Network network) {
		List<Node> pendingBackward = new LinkedList<Node>();
		TreeMap<Id, Node> clusterNodes = new TreeMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);
		pendingBackward.add(startNode);
		Set<Node> visitedNodes = new HashSet<Node>();

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			Node currNode = pendingBackward.remove(0); 
			visitedNodes.add(currNode);
			if (currNode.getInLinks() != null || ! currNode.getInLinks().isEmpty()){
				for (Link link : currNode.getInLinks().values()) {
					Node node = link.getFromNode();
					if (! visitedNodes.contains(node)) {
						pendingBackward.add(node);
						clusterNodes.put(node.getId(), node);
					}
				}
			}
		}
		return clusterNodes;
	}
	
	
	private Map<Id, Node> findForwardCluster(final Node startNode, final Network network) {
		List<Node> pendingForward = new LinkedList<Node>();
		Set<Node> visitedNodes = new HashSet<Node>();
		Map<Id, Node> clusterNodes = new HashMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);

		pendingForward.add(startNode);

		while (pendingForward.size() > 0) {
			Node currNode = pendingForward.remove(0); 
			visitedNodes.add(currNode);
			if (currNode.getOutLinks() != null || ! currNode.getOutLinks().isEmpty()) {
				for (Link link : currNode.getOutLinks().values()) {
					Node node = link.getToNode();
					if (! visitedNodes.contains(node)) {
						pendingForward.add(node);
						clusterNodes.put(node.getId(), node);
					}
				}
			}
		}
		return clusterNodes;
	}

	
	public void cleanNetwork(final Network network) {
		NetworkCleaner netCleaner = new NetworkCleaner();
		Map<Id, Node> biggestCluster = netCleaner.searchBiggestCluster(network);
		Map<Id, Node> visitedForward = new HashMap<Id, Node>();
		Map<Id, Node> visitedBackward = new HashMap<Id, Node>();
		
		log.info("searching forward...");
		for (Node node : network.getNodes().values()){
			if (biggestCluster.containsKey(node.getId()) ||  visitedForward.containsKey(node.getId())) {
				continue;
			}
			Map<Id, Node> forwardCluster = this.findForwardCluster(node, network);

			if (this.containCommonNode(forwardCluster, biggestCluster)) {
				biggestCluster.putAll(forwardCluster);
				visitedForward.putAll(forwardCluster);
			}
		}
		
		log.info("searching backward...");
		for (Node node : network.getNodes().values()){
			if (biggestCluster.containsKey(node.getId()) ||  visitedBackward.containsKey(node.getId())) {
				continue;
			}
			Map<Id, Node> backwardCluster = this.findBackwardCluster(node, network);
			if (this.containCommonNode(backwardCluster, biggestCluster)){
				biggestCluster.putAll(backwardCluster);
				visitedBackward.putAll(backwardCluster);
			}
		}
		
		netCleaner.reduceToBiggestCluster(network, biggestCluster);
	}
	
	private boolean containCommonNode(Map<Id, Node> cluster1, Map<Id, Node> cluster2) {
		for (Id id1 : cluster1.keySet()) {
			if (cluster2.containsKey(id1)) {
				return true;
			}
		}
		return false;
	}

}
