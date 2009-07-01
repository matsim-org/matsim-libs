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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

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
public class NetworkCleaner {
	
	private static final Logger log = Logger.getLogger(NetworkCleaner.class);

	/**
	 * Finds the cluster of nodes <pre>startNode</pre> is part of. The cluster
	 * contains all nodes which can be reached starting at <code>startNode</code>
	 * and from where it is also possible to return again to <code>startNode</code>.
	 *
	 * @param startNode the node to start building the cluster
	 * @param network the network the startNode is part of
	 * @return cluster of nodes <pre>startNode</pre> is part of
	 */
	private Map<Id, NodeImpl> findCluster(final NodeImpl startNode, final NetworkLayer network) {

		final Map<NodeImpl, DoubleFlagRole> nodeRoles = new HashMap<NodeImpl, DoubleFlagRole>(network.getNodes().size());

		ArrayList<NodeImpl> pendingForward = new ArrayList<NodeImpl>();
		ArrayList<NodeImpl> pendingBackward = new ArrayList<NodeImpl>();

		TreeMap<Id, NodeImpl> clusterNodes = new TreeMap<Id, NodeImpl>();
		clusterNodes.put(startNode.getId(), startNode);
		DoubleFlagRole r = getDoubleFlag(startNode, nodeRoles);
		r.forwardFlag = true;
		r.backwardFlag = true;

		pendingForward.add(startNode);
		pendingBackward.add(startNode);

		// step through the network in forward mode
		while (pendingForward.size() > 0) {
			int idx = pendingForward.size() - 1;
			NodeImpl currNode = pendingForward.remove(idx); // get the last element to prevent object shifting in the array
			for (NodeImpl node : currNode.getOutNodes().values()) {
				r = getDoubleFlag(node, nodeRoles);
				if (!r.forwardFlag) {
					r.forwardFlag = true;
					pendingForward.add(node);
				}
			}
		}

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			int idx = pendingBackward.size()-1;
			NodeImpl currNode = pendingBackward.remove(idx); // get the last element to prevent object shifting in the array
			for (NodeImpl node : currNode.getInNodes().values()) {
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

	public void run(final NetworkLayer network) {
		final Map<Id, NodeImpl> visitedNodes = new TreeMap<Id, NodeImpl>();
		Map<Id, NodeImpl> biggestCluster = new TreeMap<Id, NodeImpl>();

		log.info("running " + this.getClass().getName() + " algorithm...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + network.getNodes().size() + " nodes and " + 
				network.getLinks().size() + " links for dead-ends...");
		boolean stillSearching = true;
		Iterator<? extends NodeImpl> iter = network.getNodes().values().iterator();
		while (iter.hasNext() && stillSearching) {
			NodeImpl startNode = iter.next();
			if (!visitedNodes.containsKey(startNode.getId())) {
				Map<Id, NodeImpl> cluster = this.findCluster(startNode, network);
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
		List<NodeImpl> allNodes2 = new ArrayList<NodeImpl>(network.getNodes().values());
		for (NodeImpl node : allNodes2) {
			if (!biggestCluster.containsKey(node.getId())) {
				network.removeNode(node);		// removeNode takes care of removing links too in the network
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " + 
				network.getLinks().size() + " links.");
		log.info("done.");
	}

	private static DoubleFlagRole getDoubleFlag(final NodeImpl n, final Map<NodeImpl, DoubleFlagRole> nodeRoles) {
		DoubleFlagRole r = nodeRoles.get(n);
		if (null == r) {
			r = new DoubleFlagRole();
			nodeRoles.put(n, r);
		}
		return r;
	}

	static class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
	}

}
