/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.converters.osm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import playground.polettif.boescpa.converters.osm.networkCreator.MultimodalNetworkCreator;
import playground.polettif.boescpa.converters.osm.networkCreator.MultimodalNetworkCreatorStreets;
import playground.polettif.boescpa.converters.osm.networkCreator.OsmFilter;
import playground.polettif.boescpa.converters.osm.tools.OsmUtils;

/**
 * Provides a cleaned MATSim-Street-Network from an OSM-File
 * (for the original MATSim core version see org.matsim.core.utils.io.OsmNetworkReader).
 *
 * @author boescpa
 */
public class Osm2Network {

	// Select the MultimodalNetworkCreator to be used.
	private static MultimodalNetworkCreator getNetworkCreator(Network emptyNetwork) {
		// including pt tags:
		//return new MultimodalNetworkCreatorPT(emptyNetwork);

		// without considering OSM pt tags:
		MultimodalNetworkCreatorStreets creator = new MultimodalNetworkCreatorStreets(emptyNetwork);
		creator.addOsmFilter(new OsmFilter.OsmFilterTakeAll(6));
		// more filters are possible. see MMStreetNetworkCreatorFactory for more ideas...
		return creator;
	}

	/**
	 * Transforms an OSM-File to a cleaned MATSim-Network.
	 *
	 * @param args args[0] -> path2OsmFile, args[1] -> path2OutputNetwork
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("Wrong number of input arguments.");
		}

		String path2OsmFile = args[0];
		String path2OutputNetwork = args[1];

		convertOSMNetwork(path2OsmFile, path2OutputNetwork);
	}

	private static void convertOSMNetwork(String osmFile, String networkPath) {
		final Network network = OsmUtils.getEmptyPTScenario().getNetwork();
		getNetworkCreator(network).createMultimodalNetwork(osmFile);
		new org.matsim.core.network.algorithms.NetworkCleaner().run(network);
		new NetworkWriter(network).write(networkPath);
	}

}
