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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * This class extracts a subnetwork from a given network containing only
 * those links where at least one of the given transport modes are allowed.
 * The resulting network will not contain any links where none of the
 * specified transport modes are allowed. In addition, all links in the
 * resulting network will have at most those modes specified for the
 * extraction, additional modes are removed from the allowed set for each
 * link.<br />
 * This class makes no guarantee that the resulting network is strongly
 * connected, not even when the input network was strongly connected.
 *
 * @author mrieser
 */
public final class TransportModeNetworkFilter {

	private final Network fullNetwork;

	public TransportModeNetworkFilter(final Network fullNetwork) {
		this.fullNetwork = fullNetwork;
	}

	/**
	 * Extracts a subnetwork containing only links with the specified modes.
	 * 
	 * I had to extend this method in order to keep the nodes in the same order as in
	 * the input network. Otherwise, some router tests failed since for some from-to-pairs,
	 * multiple routes with the same costs were found. In that case, the outcome depends
	 * on the order of the nodes and links in the network. This problem might occur also
	 * in other places, therefore, I fixed it here.
	 * cdobler, sep'17
	 *
	 * @param subNetwork the network object where to store the extracted subnetwork
	 * @param extractModes set of modes that should be contained in the subnetwork
	 */
	public void filter(final Network subNetwork, final Set<String> extractModes) {	
		NetworkFactory factory = subNetwork.getFactory();

		// first, clone all nodes to ensure their order is not changed
		for (Node node : this.fullNetwork.getNodes().values()) {
			Node newNode = factory.createNode(node.getId(), node.getCoord());
			AttributesUtils.copyAttributesFromTo(node, newNode);
			subNetwork.addNode(newNode);
		}

		// second, create clones of the links allowing the extracted modes
		IdSet<Node> nodesToInclude = new IdSet<>(Node.class);
		for (Link link : this.fullNetwork.getLinks().values()) {
			Set<String> intersection = new HashSet<>(extractModes);
			intersection.retainAll(link.getAllowedModes());
			if (intersection.size() > 0) {
				Id<Node> fromId = link.getFromNode().getId();
				Id<Node> toId = link.getToNode().getId();
				Node fromNode2 = subNetwork.getNodes().get(fromId);
				Node toNode2 = subNetwork.getNodes().get(toId);
				nodesToInclude.add(fromId);
				nodesToInclude.add(toId);

				Link link2 = factory.createLink(link.getId(), fromNode2, toNode2);
				link2.setAllowedModes(intersection);
				link2.setCapacity(link.getCapacity());
				link2.setFreespeed(link.getFreespeed());
				link2.setLength(link.getLength());
				link2.setNumberOfLanes(link.getNumberOfLanes());
				NetworkUtils.setType(link2, NetworkUtils.getType(link));
				AttributesUtils.copyAttributesFromTo(link, link2);
				subNetwork.addLink(link2);
			}
		}
		
		// third, remove all nodes that are not used by the valid links
		IdSet<Node> nodesToRemove = new IdSet<>(Node.class);
		for (Node node : this.fullNetwork.getNodes().values()) {
			if (!nodesToInclude.contains(node.getId())) nodesToRemove.add(node.getId());
		}
		for (Id<Node> nodeId : nodesToRemove) subNetwork.removeNode(nodeId);
	}
}