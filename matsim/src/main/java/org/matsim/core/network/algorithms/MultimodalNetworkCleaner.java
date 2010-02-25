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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

/**
 * Variant of {@link org.matsim.core.network.algorithms.NetworkCleaner NetworkCleaner} that supports
 * multi-modal networks. If the cleaner is run, it will make sure that the sub-network of all those
 * links having at least one of the given transport modes is strongly connected. Other links are not
 * modified. If a link does not belong to the biggest cluster, the to-be-cleaned modes are removed
 * from the set of allowed modes for this link. If a link has no allowed mode anymore, it is removed
 * from the network, along with nodes that lose all their in- and out-links by that way.
 *
 * @author mrieser
 */
public class MultimodalNetworkCleaner {

	private final static Logger log = Logger.getLogger(MultimodalNetworkCleaner.class);

	private final NetworkImpl network;

	public MultimodalNetworkCleaner(final NetworkImpl network) {
		this.network = network;
	}

	/**
	 * Modifies the network such as the subnetwork containing only links that have at least
	 * one of the specified transport modes in their set of allowed transport modes is strongly
	 * connected (=every link/node can be reached by every other link/node). If multiple modes
	 * are given, the algorithm does <em>not</em> guarantee that the resulting network is strongly
	 * connected for each of the modes individually!
	 *
	 * @param modes
	 */
	public void run(final Set<TransportMode> modes) {
		final Map<Id, Link> visitedLinks = new TreeMap<Id, Link>();
		Map<Id, Link> biggestCluster = new TreeMap<Id, Link>();

		log.info("running " + this.getClass().getName() + " algorithm for modes " + Arrays.toString(modes.toArray()) + "...");

		// search the biggest cluster of nodes in the network
		log.info("  checking " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links for dead-ends...");
		boolean stillSearching = true;
		Iterator<? extends Link> iter = network.getLinks().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Link startLink = iter.next();
			if (!visitedLinks.containsKey(startLink.getId())) {
				Map<Id, Link> cluster = this.findCluster(startLink, modes);
				visitedLinks.putAll(cluster);
				if (cluster.size() > biggestCluster.size()) {
					biggestCluster = cluster;
					if (biggestCluster.size() >= (network.getLinks().size() - visitedLinks.size())) {
						// stop searching here, because we cannot find a bigger cluster in the lasting nodes
						stillSearching = false;
					}
				}
			}
		}
		log.info("    The biggest cluster consists of " + biggestCluster.size() + " nodes.");
		log.info("  done.");

		/* Remove the modes from all links not being part of the cluster. If a link has no allowed mode
		 * anymore after this, remove the link from the network.
		 */
		List<Link> allLinks = new ArrayList<Link>(this.network.getLinks().values());
		for (Link link : allLinks) {
			if (!biggestCluster.containsKey(link.getId())) {
				Set<TransportMode> reducedModes = link.getAllowedModes();
				reducedModes.removeAll(modes);
				link.setAllowedModes(reducedModes);
				if (reducedModes.isEmpty()) {
					network.removeLink(link);
					if ((link.getFromNode().getInLinks().size() + link.getFromNode().getOutLinks().size()) == 0) {
						network.removeNode(link.getFromNode());
					}
					if ((link.getToNode().getInLinks().size() + link.getToNode().getOutLinks().size()) == 0) {
						network.removeNode(link.getToNode());
					}
				}
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");
	}

	/**
	 * Finds the cluster of links <pre>startLink</pre> is part of. The cluster
	 * contains all links which can be reached starting at <code>startLink</code>
	 * and from where it is also possible to return again to <code>startLink</code>.
	 *
	 * @param startLink the link to start building the cluster
	 * @param modes the set of modes that are allowed to
	 * @return cluster of links <pre>startLink</pre> is part of
	 */
	private Map<Id, Link> findCluster(final Link startLink, final Set<TransportMode> modes) {

		final Map<Id, DoubleFlagRole> linkRoles = new HashMap<Id, DoubleFlagRole>(network.getLinks().size());

		ArrayList<Node> pendingForward = new ArrayList<Node>();
		ArrayList<Node> pendingBackward = new ArrayList<Node>();

		TreeMap<Id, Link> clusterLinks = new TreeMap<Id, Link>();

		pendingForward.add(startLink.getToNode());
		pendingBackward.add(startLink.getFromNode());

		// step through the network in forward mode
		while (pendingForward.size() > 0) {
			int idx = pendingForward.size() - 1;
			Node currNode = pendingForward.remove(idx); // get the last element to prevent object shifting in the array
			for (Link link : currNode.getOutLinks().values()) {
				if (intersectingSets(modes, link.getAllowedModes())) {
					DoubleFlagRole r = getDoubleFlag(link, linkRoles);
					if (!r.forwardFlag) {
						r.forwardFlag = true;
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
				if (intersectingSets(modes, link.getAllowedModes())) {
					DoubleFlagRole r = getDoubleFlag(link, linkRoles);
					if (!r.backwardFlag) {
						r.backwardFlag = true;
						pendingBackward.add(link.getFromNode());
						if (r.forwardFlag) {
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

	private static DoubleFlagRole getDoubleFlag(final Link l, final Map<Id, DoubleFlagRole> linkRoles) {
		DoubleFlagRole r = linkRoles.get(l.getId());
		if (null == r) {
			r = new DoubleFlagRole();
			linkRoles.put(l.getId(), r);
		}
		return r;
	}

	static class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
	}

}
