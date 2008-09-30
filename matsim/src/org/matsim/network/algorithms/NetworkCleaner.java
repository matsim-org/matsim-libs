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

package org.matsim.network.algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 * Ensures that each link in the network can be reached by any other link. 
 * Links that cannot be reached by some other links, or links from which it
 * is not possible to reach all other links, are removed from the network.
 * Nodes with no incoming or outgoing links are removed as well from the 
 * network.
 */
public class NetworkCleaner {

	private final TreeMap<Id, Node> visitedNodes = new TreeMap<Id, Node>();
	private TreeMap<Id, Node> biggestCluster = new TreeMap<Id, Node>();
	private NetworkLayer network = null;
	private int roleIndex;
	
	private static final Logger log = Logger.getLogger(NetworkCleaner.class);

	/**
	 * Finds the cluster of nodes <pre>startNode</pre> is part of. The cluster
	 * contains all nodes which can be reached starting at <code>startNode</code>
	 * and from where it is also possible to return again to <code>startNode</code>.
	 *
	 * @param startNode the node to start building the cluster
	 * @return cluster of nodes <pre>startNode</pre> is part of
	 */
	private TreeMap<Id, Node> findCluster(Node startNode) {

		for (Node node : this.network.getNodes().values()) {
			DoubleFlagRole r = (DoubleFlagRole)node.getRole(this.roleIndex);
			if (null != r) {
				r.backwardFlag = false;
				r.forwardFlag = false;
			}
		}

		ArrayList<Node> pendingForward = new ArrayList<Node>();
		ArrayList<Node> pendingBackward = new ArrayList<Node>();

		TreeMap<Id, Node> clusterNodes = new TreeMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);
		DoubleFlagRole r = getDoubleFlag(startNode);
		r.forwardFlag = true;
		r.backwardFlag = true;

		pendingForward.add(startNode);
		pendingBackward.add(startNode);

		// step through the network in forward mode
		while (pendingForward.size() > 0) {
			int idx = pendingForward.size() - 1;
			Node currNode = pendingForward.remove(idx); // get the last element to prevent object shifting in the array
			for (Node node : currNode.getOutNodes().values()) {
				r = getDoubleFlag(node);
				if (!r.forwardFlag) {
					r.forwardFlag = true;
					pendingForward.add(node);
				}
			}
		}

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			int idx = pendingBackward.size()-1;
			Node currNode = pendingBackward.remove(idx); // get the last element to prevent object shifting in the array
			for (Node node : currNode.getInNodes().values()) {
				r = getDoubleFlag(node);
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

	public void run(NetworkLayer network) {
		this.network = network;
		this.roleIndex = network.requestNodeRole();
		log.info("running " + this.getClass().getName() + " algorithm...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + network.getNodes().size() + " nodes and " + 
				network.getLinks().size() + " links for dead-ends...");
		boolean stillSearching = true;
		Iterator<? extends Node> iter = network.getNodes().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Node startNode = iter.next();
			if (!this.visitedNodes.containsKey(startNode.getId())) {
				TreeMap<Id, Node> cluster = this.findCluster(startNode);
				this.visitedNodes.putAll(cluster);
				if (cluster.size() > this.biggestCluster.size()) {
					this.biggestCluster = cluster;
					if (this.biggestCluster.size() >= (network.getNodes().size() - this.visitedNodes.size())) {
						// stop searching here, because we cannot find a bigger cluster in the lasting nodes
						stillSearching = false;
					}
				}
			}
		}
		log.info("    The biggest cluster consists of " + this.biggestCluster.size() + " nodes.");
		log.info("  done.");

		/* Reducing the network so it only contains nodes included in the biggest Cluster.
		 * Loop over all nodes and check if they are in the cluster, if not, remove them from the network
		 */
		List<Node> allNodes2 = new ArrayList<Node>(network.getNodes().values());
		for (Node node : allNodes2) {
			if (!this.biggestCluster.containsKey(node.getId())) {
				network.removeNode(node);		// removeNode takes care of removing links too in the network
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " + 
				network.getLinks().size() + " links.");
		log.info("done.");
	}

	private DoubleFlagRole getDoubleFlag(Node n) {
		DoubleFlagRole r = (DoubleFlagRole)n.getRole(this.roleIndex);
		if (null == r) {
			r = new DoubleFlagRole();
			n.setRole(this.roleIndex, r);
		}
		return r;
	}

	static private class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
		
		public DoubleFlagRole() {
			// make constructor public in private class
		}
	}

}
