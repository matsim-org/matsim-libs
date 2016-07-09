/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.CollectionUtils;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.Set;

/**
 * Workbench. Runs the network cleaner on the "road" links of the given network.
 */
public class CleanRoadsMultimodalNetwork {

	public static void main(String[] args) {
		Network network = NetworkTools.readNetwork(args[0]);

		Set<String> roadModes = CollectionUtils.stringToSet("car,bus");
		Network roadNetwork = NetworkTools.filterNetworkByLinkMode(network, roadModes);
		Network restNetwork = NetworkTools.filterNetworkExceptLinkMode(network, roadModes);
		new NetworkCleaner().run(roadNetwork);
		NetworkTools.integrateNetwork(roadNetwork, restNetwork);
		NetworkTools.writeNetwork(roadNetwork, args[0]);
	}
}
