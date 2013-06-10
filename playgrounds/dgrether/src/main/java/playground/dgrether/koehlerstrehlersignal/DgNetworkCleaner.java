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

package playground.dgrether.koehlerstrehlersignal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;

public class DgNetworkCleaner implements NetworkRunnable {

	private static final Logger log = Logger.getLogger(DgNetworkCleaner.class);

	private Map<Id, Node> findBackwardCluster(final Node startNode, final Network network) {
		List<Node> pendingBackward = new LinkedList<Node>();
		TreeMap<Id, Node> clusterNodes = new TreeMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);
		pendingBackward.add(startNode);

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			Node currNode = pendingBackward.remove(0); 
			if (currNode.getInLinks() != null || ! currNode.getInLinks().isEmpty()){
				for (Link link : currNode.getInLinks().values()) {
					Node node = link.getFromNode();
					pendingBackward.add(node);
					clusterNodes.put(node.getId(), node);
				}
			}
		}
		return clusterNodes;
	}
	
	
	private Map<Id, Node> findForwardCluster(final Node startNode, final Network network) {
		List<Node> pendingForward = new LinkedList<Node>();

		TreeMap<Id, Node> clusterNodes = new TreeMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);

		pendingForward.add(startNode);

		while (pendingForward.size() > 0) {
			Node currNode = pendingForward.remove(0); 
			if (currNode.getOutLinks() != null || ! currNode.getOutLinks().isEmpty()) {
				for (Link link : currNode.getOutLinks().values()) {
					Node node = link.getToNode();
					pendingForward.add(node);
					clusterNodes.put(node.getId(), node);
				}
			}
		}
		return clusterNodes;
	}

	@Override
	public void run(final Network network) {
		final Map<Id, Node> visitedNodes = new TreeMap<Id, Node>();
		Map<Id, Node> biggestCluster = new TreeMap<Id, Node>();

		log.info("running " + this.getClass().getName() + " algorithm...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links for dead-ends...");
		boolean stillSearching = true;
		Iterator<? extends Node> iter = network.getNodes().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Node startNode = iter.next();
			if (!visitedNodes.containsKey(startNode.getId())) {
				Map<Id, Node> cluster = this.findForwardCluster(startNode, network);
				visitedNodes.putAll(cluster);
				if (cluster.size() > biggestCluster.size()) {
					biggestCluster = cluster;
					if (biggestCluster.size() >= (network.getNodes().size() - visitedNodes.size())) {
						// stop searching here, because we cannot find a bigger cluster in the lasting nodes
						stillSearching = false;
					}
				}
			}
		}
		log.info("    The biggest cluster consists of " + biggestCluster.size() + " nodes.");
		log.info("  done.");

		/* Reducing the network so it only contains nodes included in the biggest Cluster.
		 * Loop over all nodes and check if they are in the cluster, if not, remove them from the network
		 */
		List<Node> allNodes2 = new ArrayList<Node>(network.getNodes().values());
		for (Node node : allNodes2) {
			if (!biggestCluster.containsKey(node.getId())) {
				network.removeNode(node.getId());		// removeNode takes care of removing links too in the network
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");
	}


}
