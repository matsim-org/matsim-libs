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

package playground.polettif.publicTransitMapping.osm;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.Collections;

/**
 * A version of MultimodalNetworkCreator that retains the pt tags in the mode tag of the network.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorPT implements MultimodalNetworkCreator {

	private final CoordinateTransformation transformation;
	private Network network;

	/**
	 * Creates a MATSim network file from osm.
	 * @param args [0] osm file
	 *             [1] output MATSim network file.
	 *             [2] output coordinate system (optional)
	 */
	public static void main(String[] args) {
		if(args.length == 2) {
			run(args[0], args[1], null);
		} else if(args.length == 3) {
			run(args[0], args[1], args[2]);
		} else {
			throw new IllegalArgumentException("Wrong number parameters.");
		}
	}

	/**
	 * Creates a MATSim network file from osm.
	 * @param osmFile the osm file
	 * @param outputNetworkFile the filepath to the output MATSim network file
	 * @param outputCoordinateSystem output coordinate System (no transformation used if <tt>null</tt>)
	 */
	public static void run(String osmFile, String outputNetworkFile, String outputCoordinateSystem) {
		Network network = NetworkTools.createNetwork();

		CoordinateTransformation transformation = (outputCoordinateSystem != null ?
				TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem) : null);

		new MultimodalNetworkCreatorPT(network, transformation).createMultimodalNetwork(osmFile);

		// clean up networks
		Network streetNetwork = NetworkTools.filterNetworkByLinkMode(network, Collections.singleton(TransportMode.car));
		Network restNetwork = NetworkTools.filterNetworkExceptLinkMode(network, Collections.singleton(TransportMode.car));
		new NetworkCleaner().run(streetNetwork);
		NetworkTools.integrateNetwork(streetNetwork, restNetwork);

		NetworkTools.writeNetwork(streetNetwork, outputNetworkFile);
	}

	public MultimodalNetworkCreatorPT(Network network, CoordinateTransformation transformation) {
		this.network = network;
		this.transformation = (transformation == null ? new IdentityTransformation() : transformation);
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReaderWithPT osmReader = new OsmNetworkReaderWithPT(this.network, transformation, true);
		osmReader.setKeepPaths(false);
		osmReader.setMaxLinkLength(400.0);
		osmReader.parse(osmFile);
	}
}
