/* *********************************************************************** *
 * project: org.matsim.*
 * PreProcessDijkstra.java
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

package org.matsim.router.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;

/**
 * Pre-processes a given network, gathering information which
 * can be used by a Dijkstra when computing least-cost paths
 * between a start and an end node. Specifically, marks the nodes
 * that are in dead-end, i.e. that are connected to the rest of
 * the network by a single 'entry' node only.
 *
 * @author lnicolas
 */
public class PreProcessDijkstra extends NetworkAlgorithm {

	private static final Logger log = Logger.getLogger(PreProcessDijkstra.class);

	/**
	 * The role this algorithm uses to store its data in the network.
	 */
	int roleIndex = -1;

	boolean containsData = false;

	@Override
	public void run(final NetworkLayer network) {
		if (this.roleIndex == -1) {
			this.roleIndex = network.requestNodeRole();
		}
		markDeadEnds(network);
		this.containsData = true;
	}

	/**
	 * Marks nodes that are in dead ends (i.e. nodes that are connected through a single node to the rest of the network), starting at all nodes with degree 2.
	 * @param network
	 * The network on which to process.
	 */
	protected void markDeadEnds(final NetworkLayer network) {
		long now = System.currentTimeMillis();

		DeadEndRole role;
		for (Node node : network.getNodes().values()) {
			role = getRole(node);

			Map<Id, ? extends Node> incidentNodes = node.getIncidentNodes();
			if (incidentNodes.size() == 1) {
				ArrayList<Node> deadEndNodes = new ArrayList<Node>();

				while (role.getInDeadEndCount() == incidentNodes.size() - 1) {

					deadEndNodes.add(node);
					deadEndNodes.addAll(role.getDeadEndNodes());
					role.getDeadEndNodes().clear();

					// Just set a dummy DeadEndEntryNode, such that is
					// isn't null. We use this as a flag to determine
					// whether we already processed this node.
					role.setDeadEndEntryNode(node);

					Iterator<? extends Node> it = incidentNodes.values().iterator();
					while (role.getDeadEndEntryNode() != null && it.hasNext()) {
						node = it.next();
						role = getRole(node);
					}
					if (role.getDeadEndEntryNode() == null) {
						role.incrementInDeadEndCount();
						incidentNodes = node.getIncidentNodes();
					} else {
						log.error("All " + incidentNodes.size() + " incident nodes of node " + node.getId() + " are dead ends!");
						return;
					}
				}
				role.getDeadEndNodes().addAll(deadEndNodes);
			}
		}

		// Now set the proper deadEndEntryNode for each node
		int deadEndNodeCount = 0;
		for (Node node : network.getNodes().values()) {
			role = getRole(node);
			for (Node n : role.getDeadEndNodes()) {
				DeadEndRole r = getRole(n);
				r.setDeadEndEntryNode(node);
				deadEndNodeCount++;
			}
			role.getDeadEndNodes().clear();
		}
		log.info("nodes in dead ends: " + deadEndNodeCount
				+ " (total nodes: " + network.getNodes().size() + "). Done in "
				+ (System.currentTimeMillis() - now) + " ms");
	}

	/**
	 * Returns the role for the given Node. Creates a new Role if none exists yet.
	 * @param n The Node for which to create a role.
	 * @return The role for the given Node
	 */
	DeadEndRole getRole(final Node n) {
		DeadEndRole r = (DeadEndRole) n.getRole(this.roleIndex);
		if (null == r) {
			r = new DeadEndRole();
			n.setRole(this.roleIndex, r);
		}
		return r;
	}

	/**
	 * @return The index of the roles associated to each node
	 * that are used to store the information of the pre processing.
	 */
	public int roleIndex() {
		return this.roleIndex;
	}

	/**
	 * Contains information whether the associated node is in a dead end.
	 * @author lnicolas
	 */
	public class DeadEndRole {
		Node deadEndEntryNode = null;

		int inDeadEndCount = 0;

		ArrayList<Node> deadEndNodes = new ArrayList<Node>();

		ArrayList<Node> getDeadEndNodes() {
			return this.deadEndNodes;
		}

		/**
		 * @return the inDeadEndCount
		 */
		private int getInDeadEndCount() {
			return this.inDeadEndCount;
		}

		/**
		 * @param inDeadEndCount
		 *            the inDeadEndCount to set
		 */
		private void incrementInDeadEndCount() {
			this.inDeadEndCount++;
		}


		/**
		 * @return The node that connects the associated node to the rest
		 * of the network (i.e. without the deadEndEntryNode, the associated
		 * node would not be connected to the network) and null otherwise
		 * (i.e. if the associated node is not in a dead end).
		 */
		public Node getDeadEndEntryNode() {
			return this.deadEndEntryNode;
		}

		private void setDeadEndEntryNode(final Node node) {
			this.deadEndEntryNode = node;
		}
	}

	public boolean containsData() {
		return this.containsData;
	}
}