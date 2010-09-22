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

package playground.andreas.intersection.dijkstra;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

public class NetworkWrapper {

	final private static Logger log = Logger.getLogger(NetworkWrapper.class);

	/**
	 * Converts a network to an inverted network. Inverted nodes are situated at
	 * the end of the real link. Inverted link attributes are copied from toLink
	 * of the real network, thus every inverted link actually starts at the
	 * location of a real node.
	 *
	 * @param networkLayer
	 *            The real network
	 * @return The converted network
	 */
	public static NetworkImpl wrapNetwork(Network networkLayer) {

		NetworkImpl wrappedNetwork = NetworkImpl.createNetwork();
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;

		for (Link link : networkLayer.getLinks().values()) {
			wrappedNetwork.createAndAddNode(link.getId(), link.getToNode().getCoord());
			numberOfNodesGenerated++;
		}

		for (Node node : networkLayer.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					Link link = wrappedNetwork.createAndAddLink(new IdImpl(numberOfLinksGenerated),
							wrappedNetwork.getNodes().get(inLink.getId()), wrappedNetwork.getNodes().get(new IdImpl(outLink.getId().toString())),
							outLink.getLength(),
							outLink.getFreespeed(),
							outLink.getCapacity(),
							outLink.getNumberOfLanes());
					((LinkImpl) link).setType(((LinkImpl) outLink).getType());
					numberOfLinksGenerated++;
				}
			}
		}

		log.info("Generated " + numberOfNodesGenerated + " Nodes and " + numberOfLinksGenerated + " Links");

		// Debug only
		// NetworkWriter myNetworkWriter = new NetworkWriter(wrappedNetwork,
		// "wrappedNetwork");
		// myNetworkWriter.write();

		return wrappedNetwork;
	}

}
