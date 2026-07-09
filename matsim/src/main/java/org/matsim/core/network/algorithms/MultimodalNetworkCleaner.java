/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * Variant of {@link org.matsim.core.network.algorithms.NetworkCleaner NetworkCleaner} that supports
 * multi-modal networks. If the cleaner is run, it will make sure that the sub-network of all those
 * links having at least one of the given transport modes is strongly connected. Other links are not
 * modified. If a link does not belong to the biggest cluster, the to-be-cleaned modes are removed
 * from the set of allowed modes for this link. If a link has no allowed mode anymore, it is removed
 * from the network, along with nodes that lose all their in- and out-links by that way.
 *
 * @deprecated not deprecated, but you should really use {@link NetworkUtils#cleanNetwork(Network, Set)}
 * @author mrieser
 */
public final class MultimodalNetworkCleaner {

	private final static Logger log = LogManager.getLogger(MultimodalNetworkCleaner.class);

	private final Network network;

	private final Set<Id<Link>> removedLinks = new HashSet<>();
	private final Set<Id<Link>> modifiedLinks = new HashSet<>();

	public MultimodalNetworkCleaner(final Network network) {
		this.network = network;
	}

	/**
	 * Modifies the network such that the subnetwork containing only links that have at least
	 * one of the specified transport modes in their set of allowed transport modes is strongly
	 * connected (=every link/node can be reached by every other link/node). If multiple modes
	 * are given, the algorithm does <em>not</em> guarantee that the resulting network is strongly
	 * connected for each of the modes individually! Nodes having links connected to them before
	 * cleaning, but none after cleaning, are removed from the network.
	 *
	 * @param modes
	 */
	public void run(final Set<String> modes) {
		run(modes, new HashSet<String>());
	}

	/**
	 * Modifies the network such that the subnetwork containing only links that have at least
	 * one of the specified transport modes (<code>cleaningModes</code> as well as
	 * <code>connectivityModes</code>) in their set of allowed transport modes is strongly
	 * connected (=every link/node can be reached by every other link/node). In contrast to
	 * {@link #run(Set)}, this method will only remove <code>cleaningModes</code> from links,
	 * but not <code>connectivityModes</code>. Thus, the resulting  network may still contain
	 * nodes that are sources or sinks for modes of <code>connectivityModes</code>, but not
	 * for modes of <code>cleaningModes</code> and <code>connectivityModes</code> combined. If
	 * multiple modes are given as <code>cleaningModes</code>, the algorithm does <em>not</em>
	 * guarantee that the resulting network is strongly connected for each of the modes
	 * individually! The subnetwork consisting of links having only modes from
	 * <code>cleaningModes</code> may not be strongly connected, only in combination with links
	 * having modes from <code>connectivityModes</code>, a strongly connected subnetwork emerges.
	 * Nodes having links connected to them before cleaning, but none after cleaning, are removed
	 * from the network.
	 *
	 * @param cleaningModes
	 * @param connectivityModes
	 */
	public void run(final Set<String> cleaningModes, final Set<String> connectivityModes) {
		final Set<String> combinedModes = new HashSet<>(cleaningModes);
		combinedModes.addAll(connectivityModes);
		final Map<Id<Link>, Link> visitedLinks = new TreeMap<>();
		Map<Id<Link>, Link> biggestCluster = new TreeMap<>();

		log.info("running " + this.getClass().getName() + " algorithm for modes " + Arrays.toString(cleaningModes.toArray())
				+ " with connectivity modes " + Arrays.toString(connectivityModes.toArray()) + "...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links for dead-ends...");

		// Pre-compute the set of link IDs whose allowedModes intersect combinedModes. This way the
		// BFS hot loop in findCluster only has to do a single HashSet#contains lookup per edge visit
		// instead of allocating a fresh HashSet/HashMap iterator inside intersectingSets each time.
		// (For large networks this set-intersection check dominated the runtime; see profile.)
		final Set<Id<Link>> modeLinkIds = new HashSet<>();
		for (Link link : this.network.getLinks().values()) {
			if (intersectingSets(combinedModes, link.getAllowedModes())) {
				modeLinkIds.add(link.getId());
			}
		}

		boolean stillSearching = true;
		Iterator<? extends Link> iter = this.network.getLinks().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Link startLink = iter.next();
			if ((!visitedLinks.containsKey(startLink.getId())) && modeLinkIds.contains(startLink.getId())) {
				Map<Id<Link>, Link> cluster = this.findCluster(startLink, modeLinkIds);
				visitedLinks.putAll(cluster);
				if (cluster.size() > biggestCluster.size()) {
					biggestCluster = cluster;
					// upper bound: only mode-matching, not-yet-visited links can ever join a cluster
					if (biggestCluster.size() >= (modeLinkIds.size() - visitedLinks.size())) {
						// stop searching here, because we cannot find a bigger cluster in the lasting nodes
						stillSearching = false;
					}
				}
			}
		}
		log.info("    The biggest cluster consists of " + biggestCluster.size() + " links.");
		log.info("  done.");

		/* Remove the modes from all links not being part of the cluster. If a link has no allowed mode
		 * anymore after this, remove the link from the network.
		 */
		List<Link> allLinks = new ArrayList<>(this.network.getLinks().values());
		for (Link link : allLinks) {
			if (!biggestCluster.containsKey(link.getId())) {
				Set<String> reducedModes = new HashSet<>(link.getAllowedModes());
				reducedModes.removeAll(cleaningModes);
				link.setAllowedModes(reducedModes);
				if (reducedModes.isEmpty()) {
					this.network.removeLink(link.getId());
					if ((link.getFromNode().getInLinks().size() + link.getFromNode().getOutLinks().size()) == 0) {
						this.network.removeNode(link.getFromNode().getId());
					}
					if ((link.getToNode().getInLinks().size() + link.getToNode().getOutLinks().size()) == 0) {
						this.network.removeNode(link.getToNode().getId());
					}
					this.removedLinks.add(link.getId());
				}
				if(!removedLinks.contains(link.getId())) modifiedLinks.add(link.getId());
			}
		}
		log.info("  resulting network contains " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links.");
		log.info("done.");
	}

	/**
	 * Finds the cluster of links <pre>startLink</pre> is part of. The cluster
	 * contains all links which can be reached starting at <code>startLink</code>
	 * and from where it is also possible to return again to <code>startLink</code>.
	 *
	 * @param startLink the link to start building the cluster
	 * @param modeLinkIds the set of link IDs whose allowed modes intersect the modes being cleaned
	 *        (pre-computed by {@link #run(Set, Set)} so the BFS does not have to repeat the
	 *        set-intersection check per edge visit)
	 * @return cluster of links <pre>startLink</pre> is part of
	 */
	private Map<Id<Link>, Link> findCluster(final Link startLink, final Set<Id<Link>> modeLinkIds) {

		// Per-link forward/backward reachability flags, indexed by Id#index() instead of a
		// HashMap<Id<Link>, ...>. This turns the per-edge-visit lookup in the BFS below into a plain array access
		// (the HashMap get/put previously dominated the runtime on large networks) and avoids allocating a role
		// object per visited link.
		final int linkIdCount = Id.getNumberOfIds(Link.class);
		final boolean[] forwardFlags = new boolean[linkIdCount];
		final boolean[] backwardFlags = new boolean[linkIdCount];

		ArrayList<Node> pendingForward = new ArrayList<>();
		ArrayList<Node> pendingBackward = new ArrayList<>();

		TreeMap<Id<Link>, Link> clusterLinks = new TreeMap<>();

		pendingForward.add(startLink.getToNode());
		pendingBackward.add(startLink.getFromNode());

		// step through the network in forward mode
		while (pendingForward.size() > 0) {
			int idx = pendingForward.size() - 1;
			Node currNode = pendingForward.remove(idx); // get the last element to prevent object shifting in the array
			for (Link link : currNode.getOutLinks().values()) {
				if (modeLinkIds.contains(link.getId())) {
					int linkIndex = link.getId().index();
					if (!forwardFlags[linkIndex]) {
						forwardFlags[linkIndex] = true;
						pendingForward.add(link.getToNode());
					}
				}
			}
		}

		// now step through the network in backward mode
		while (pendingBackward.size() > 0) {
			int idx = pendingBackward.size()-1;
			Node currNode = pendingBackward.remove(idx); // get the last element to prevent object shifting in the array
			for (Link link : currNode.getInLinks().values()) {
				if (modeLinkIds.contains(link.getId())) {
					int linkIndex = link.getId().index();
					if (!backwardFlags[linkIndex]) {
						backwardFlags[linkIndex] = true;
						pendingBackward.add(link.getFromNode());
						if (forwardFlags[linkIndex]) {
							// the node can be reached forward and backward, add it to the cluster
							clusterLinks.put(link.getId(), link);
						}
					}
				}
			}
		}

		return clusterLinks;
	}

	/**
	 * @return the removedLinks
	 */
	public final Set<Id<Link>> getRemovedLinkIds() {
		return removedLinks;
	}

	/**
	 * @return the modifiedLinks
	 */
	public final Set<Id<Link>> getModifiedLinkIds() {
		return modifiedLinks;
	}

	/**
	 * An optimized method to find out if two sets have common elements.
	 * The basic approach would be to copy one set, and then use
	 * {@link Set#retainAll(java.util.Collection)} and see if the
	 * resulting set is not empty. But that way creates additional objects,
	 * which this method tries to avoid.
	 *
	 * @param <T> the type of objects in the two sets
	 * @param setA the first set
	 * @param setB the second set
	 * @return <code>true</code> if the intersection of two sets is not empty
	 */
	private <T> boolean intersectingSets(final Set<T> setA, final Set<T> setB) {
		for (T t : setA) {
			if (setB.contains(t)) {
				return true;
			}
		}
		return false;
	}

}
