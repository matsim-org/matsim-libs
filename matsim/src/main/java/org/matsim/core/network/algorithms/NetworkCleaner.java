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

package org.matsim.core.network.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;

/**
 * Ensures that each link in the network can be reached by any other link.
 * Links that cannot be reached by some other links, or links from which it
 * is not possible to reach all other links, are removed from the network.
 * Nodes with no incoming or outgoing links are removed as well from the
 * network.
 *
 * @author mrieser
 * @author balmermi
 */
public final class NetworkCleaner implements NetworkRunnable {

	private static final Logger log = LogManager.getLogger(NetworkCleaner.class);

	/**
	 * Finds the cluster of nodes of which <code>startNode</code> is part of. The cluster
	 * contains all nodes which can be reached starting at <code>startNode</code>
	 * and from where it is also possible to return again to <code>startNode</code>.
	 *
	 * @param startNode the node to start building the cluster
	 * @param network the network the startNode is part of
	 * @return cluster of nodes of which <code>startNode</code> is part of
	 */
	private Map<Id<Node>, Node> findCluster(final Node startNode, final Network network) {

		final Map<Node, DoubleFlagRole> nodeRoles = new HashMap<>(network.getNodes().size());

		ArrayList<Node> pendingForward = new ArrayList<>();
		ArrayList<Node> pendingBackward = new ArrayList<>();

		TreeMap<Id<Node>, Node> clusterNodes = new TreeMap<>();
		clusterNodes.put(startNode.getId(), startNode);
		DoubleFlagRole r = getDoubleFlag(startNode, nodeRoles);
		r.forwardFlag = true;
		r.backwardFlag = true;

		pendingForward.add(startNode);
		pendingBackward.add(startNode);

		// step through the network in forward mode
		while (!pendingForward.isEmpty()) {
			Node currNode = pendingForward.removeLast(); // get the last element to prevent object shifting in the array
			for (Link link : currNode.getOutLinks().values()) {
				Node node = link.getToNode();
				r = getDoubleFlag(node, nodeRoles);
				if (!r.forwardFlag) {
					r.forwardFlag = true;
					pendingForward.add(node);
				}
			}
		}

		// now step through the network in backward mode
		while (!pendingBackward.isEmpty()) {
			Node currNode = pendingBackward.removeLast(); // get the last element to prevent object shifting in the array
			for (Link link : currNode.getInLinks().values()) {
				Node node = link.getFromNode();
				r = getDoubleFlag(node, nodeRoles);
				if (!r.backwardFlag) {
					r.backwardFlag = true;
					pendingBackward.add(node);
					if (r.forwardFlag) {
						// the node can be reached forward and backward, add it to the cluster
						clusterNodes.put(node.getId(), node);
					}
				}
			}
		}

		return clusterNodes;
	}

	/**
	 * Searches the biggest cluster in the given Network. The Network is not modified.
	 */
	private Map<Id<Node>, Node> searchBiggestCluster(Network network) {
		final Map<Id<Node>, Node> visitedNodes = new TreeMap<>();
		Map<Id<Node>, Node> biggestCluster = new TreeMap<>();

		log.info("running " + this.getClass().getName() + " algorithm...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links for dead-ends...");
		boolean stillSearching = true;
		Iterator<? extends Node> iter = network.getNodes().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Node startNode = iter.next();
			if (!visitedNodes.containsKey(startNode.getId())) {
				Map<Id<Node>, Node> cluster = this.findCluster(startNode, network);
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
		return biggestCluster;
	}

	/**
	 * Reducing the network so it only contains nodes included in the biggest Cluster.
	 * Loop over all nodes and check if they are in the cluster, if not, remove them from the network
	 */
	private static void reduceToBiggestCluster(Network network, Map<Id<Node>, Node> biggestCluster) {
		List<Node> allNodes2 = new ArrayList<>(network.getNodes().values());
		for (Node node : allNodes2) {
			if (!biggestCluster.containsKey(node.getId())) {
				network.removeNode(node.getId());		// removeNode takes care of removing links too in the network
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");
	}

	@Override
	public void run(final Network network) {
		Map<Id<Node>, Node> biggestCluster = this.searchBiggestCluster(network);
		reduceToBiggestCluster(network, biggestCluster);
	}

	private static DoubleFlagRole getDoubleFlag(final Node n, final Map<Node, DoubleFlagRole> nodeRoles) {
		DoubleFlagRole r = nodeRoles.get(n);
		if (null == r) {
			r = new DoubleFlagRole();
			nodeRoles.put(n, r);
		}
		return r;
	}

	static class DoubleFlagRole {
		boolean forwardFlag = false;
		boolean backwardFlag = false;
	}

}
