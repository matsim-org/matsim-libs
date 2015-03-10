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

package playground.boescpa.converters.osm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.run.NetworkCleaner;
import playground.boescpa.converters.osm.networkCreator.*;
import playground.boescpa.converters.osm.tools.OsmUtils;

/**
 * Provides a cleaned MATSim-Street-Network from an OSM-File.
 *
 * @author boescpa
 */
public class Osm2Network {

	// Select the MultimodalNetworkCreator to be used.
	private static MultimodalNetworkCreator getNetworkCreator(Network emptyNetwork) {
		//return new MultimodalNetworkCreatorRetainingPTTags(emptyNetwork);
		//return new MultimodalNetworkCreatorRectangleAroundSwitzerland(emptyNetwork);
		return new MultimodalNetworkCreatorEllipseAroundSwitzerland(emptyNetwork);
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
		new NetworkWriter(network).write(networkPath);
		new NetworkCleaner().run(networkPath, networkPath);
	}

}
