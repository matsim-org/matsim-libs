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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

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
	 * @param subNetwork the network object where to store the extracted subnetwork
	 * @param extractModes set of modes that should be contained in the subnetwork
	 */
	public void filter(final Network subNetwork, final Set<String> extractModes) {
		NetworkFactory factory = subNetwork.getFactory();
		for (Link link : this.fullNetwork.getLinks().values()) {
			Set<String> intersection = new HashSet<>(extractModes);
			intersection.retainAll(link.getAllowedModes());
			if (intersection.size() > 0) {
				Id<Node> fromId = link.getFromNode().getId();
				Id<Node> toId = link.getToNode().getId();
				Node fromNode2 = subNetwork.getNodes().get(fromId);
				Node toNode2 = subNetwork.getNodes().get(toId);
				if (fromNode2 == null) {
					fromNode2 = factory.createNode(fromId, link.getFromNode().getCoord());
					subNetwork.addNode(fromNode2);
					if (fromId == toId) {
						toNode2 = fromNode2;
					}
				}
				if (toNode2 == null) {
					toNode2 = factory.createNode(toId, link.getToNode().getCoord());
					subNetwork.addNode(toNode2);
				}

				Link link2 = factory.createLink(link.getId(), fromNode2, toNode2);
				link2.setAllowedModes(intersection);
				link2.setCapacity(link.getCapacity());
				link2.setFreespeed(link.getFreespeed());
				link2.setLength(link.getLength());
				link2.setNumberOfLanes(link.getNumberOfLanes());
				((LinkImpl) link2).setType(((LinkImpl) link).getType());
				subNetwork.addLink(link2);
			}
		}
	}

}
