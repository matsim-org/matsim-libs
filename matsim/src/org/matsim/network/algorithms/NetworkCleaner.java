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
import java.util.TreeMap;

import org.matsim.basic.v01.BasicNodeSet;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;

public class NetworkCleaner extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final TreeMap<IdI, Node> visitedNodes = new TreeMap<IdI, Node>();
	private TreeMap<IdI, Node> biggestCluster = new TreeMap<IdI, Node>();
	private NetworkLayer network = null;
	private int roleIndex;
	private boolean renumber;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkCleaner() {
		this(false);
	}

	public NetworkCleaner(boolean renumber) {
		super();
		this.renumber = renumber;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////


	/**
	 * finds the cluster <pre>startNode</pre> is part of. The cluster contains
	 * all nodes which can be reached starting at startNode.
	 *
	 * @param startNode the node to start building the cluster
	 * @return cluster of nodes <pre>startNode</pre> is part of
	 */
	private TreeMap<IdI, Node> findCluster(Node startNode) {

		Iterator<?> nIter = this.network.getNodes().iterator();
		while (nIter.hasNext()) {
			Node n = (Node)nIter.next();
			DoubleFlagRole r = (DoubleFlagRole)n.getRole(this.roleIndex);
			if (null != r) {
				r.backwardFlag = false;
				r.forwardFlag = false;
			}
		}

		ArrayList<Node> pendingForward = new ArrayList<Node>();
		ArrayList<Node> pendingBackward = new ArrayList<Node>();

		TreeMap<IdI, Node> clusterNodes = new TreeMap<IdI, Node>();
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
			Iterator<?> iter = currNode.getOutNodes().iterator();
			while (iter.hasNext()) {
				Node n = (Node)iter.next();
				r = getDoubleFlag(n);
				if (!r.forwardFlag) {
					r.forwardFlag = true;
					pendingForward.add(n);
				}
			}
		}

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			int idx = pendingBackward.size()-1;
			Node currNode = pendingBackward.remove(idx); // get the last element to prevent object shifting in the array
			Iterator<?> iter = currNode.getInNodes().iterator();
			while (iter.hasNext()) {
				Node n = (Node)iter.next();
				r = getDoubleFlag(n);
				if (!r.backwardFlag) {
					r.backwardFlag = true;
					pendingBackward.add(n);
					if (r.forwardFlag) {
						// the node can be reached forward and backward, add it to the cluster
						clusterNodes.put(n.getId(), n);
					}
				}
			}
		}

		return clusterNodes;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(NetworkLayer network) {
		this.network = network;
		this.roleIndex = network.requestNodeRole();
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		System.out.println("      checking " + network.getNodes().size() + " nodes for dead-ends...");

		// search the biggest cluster of nodes in the network
		boolean stillSearching = true;
		Iterator<?> iter = network.getNodes().iterator();
		while (iter.hasNext() && stillSearching) {
			Node startNode = (Node)iter.next();
			if (!this.visitedNodes.containsKey(startNode.getId())) {
				TreeMap<IdI, Node> cluster = this.findCluster(startNode);
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

		System.out.println("        The biggest cluster consists of " + this.biggestCluster.size() + " nodes.");
		System.out.println("      done.");

		// reducing the network so it only contains nodes included in the biggest Cluster
		// loop over all nodes and check if they are in the cluster, if not, remove them from the network
		BasicNodeSetI allNodes2 = new BasicNodeSet();
		allNodes2.addAll(network.getNodes());
		Iterator<?> allNIter2 = allNodes2.iterator();
		while (allNIter2.hasNext()) {
			Node node = (Node)allNIter2.next();
			if (!this.biggestCluster.containsKey(node.getId())) {
				network.removeNode(node);		// removeNode takes care of removing links too in the network
			}
		}

		// renumber links and nodes if requested
		if (this.renumber) {
			int id = 1;
			Iterator<?> nIter = network.getNodes().iterator();
			while (nIter.hasNext()) {
				Node node = (Node)nIter.next();
				node.setOrigId(node.getId().toString());
				node.setId(new Id(id));
				id++;
			}

			id = 1;
			Iterator<?> lIter = network.getLinks().iterator();
			while (lIter.hasNext()) {
				Link link = (Link)lIter.next();
				link.setOrigId(link.getId().toString());
				link.setId(new Id(id));
				id++;
			}
		}

		System.out.println("    done.");
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
		public boolean forwardFlag = false;
		public boolean backwardFlag = false;
	};

}
