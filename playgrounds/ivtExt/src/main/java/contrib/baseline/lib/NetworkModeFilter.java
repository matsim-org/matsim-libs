/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package contrib.baseline.lib;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Filters a network for a given mode.
 * Call arguments: pathToOriginalNetwork mode pathToOutputNetwork
 *
 * @author boescpa
 */
public class NetworkModeFilter {

	public static void main (final String[] args) {
		Network network = NetworkUtils.readNetwork(args[0]);
		Network networkOnlyMode = org.matsim.core.network.NetworkUtils.createNetwork();
		NetworkFactory factory = networkOnlyMode.getFactory();

		network.getLinks().values().forEach(link -> {
			if (link.getAllowedModes().contains(args[1])) {
				if (!networkOnlyMode.getNodes().containsKey(link.getFromNode().getId())) {
					Node node = network.getNodes().get(link.getFromNode().getId());
					Node nodeCopy = factory.createNode(node.getId(), node.getCoord());
					networkOnlyMode.addNode(nodeCopy);
				}
				if (!networkOnlyMode.getNodes().containsKey(link.getToNode().getId())) {
					Node node = network.getNodes().get(link.getToNode().getId());
					Node nodeCopy = factory.createNode(node.getId(), node.getCoord());
					networkOnlyMode.addNode(nodeCopy);
				}
				networkOnlyMode.addLink(link);
			}
		});

		new NetworkCleaner().run(networkOnlyMode);
		new NetworkWriter(networkOnlyMode).write(args[2]);
	}
}
