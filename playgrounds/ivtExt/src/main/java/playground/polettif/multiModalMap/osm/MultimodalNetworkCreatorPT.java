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

package playground.polettif.multiModalMap.osm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.multiModalMap.tools.NetworkTools;

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
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();

		CoordinateTransformation transformation = (args.length == 3 ? TransformationFactory.getCoordinateTransformation("WGS84", args[2]) : null);

		new MultimodalNetworkCreatorPT(network, transformation).createMultimodalNetwork(args[0]);
		NetworkTools.writeNetwork(network, args[1]);
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
