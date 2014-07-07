/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.osm.procedures;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import playground.boescpa.converters.osm.tools.Osm2TransitSimplified;
import playground.scnadine.converters.osmPT.Osm2TransitLines;

/**
 * The default implementation of MultimodalNetworkCreator.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorDefault extends MultimodalNetworkCreator {

	public MultimodalNetworkCreatorDefault(Network network) {
		super(network);
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		Network streetNetwork = createStreetNetwork(osmFile);
		Network ptNetwork = createPTNetwork(osmFile);
		mergePTwithCarNetwork(this.network, streetNetwork, ptNetwork);
	}

	/**
	 * Creates a standard car network from the given osmFile using org.matsim.core.utils.io.OsmNetworkReader.
	 *
	 * @param osmFile
	 * @return Standard car network.
	 */
	private Network createStreetNetwork(String osmFile) {
		log.info("Creating street network from osmFile...");

		Network network = NetworkUtils.createNetwork();

		// Setting up instance of org.matsim.core.utils.io.OsmNetworkReader:
		OsmNetworkReader osmReader =
				new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"), false);
		osmReader.setKeepPaths(true); // The full network is needed for merging.
		osmReader.setMemoryOptimization(true);

		log.info("	Setting parameters for osmReader...");
		osmReader.setHighwayDefaults(1, "motorway", 2, 120.0 / 3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1, 80.0 / 3.6, 1.0, 1500, true);
		osmReader.setHighwayDefaults(2, "trunk", 1, 80.0 / 3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(2, "trunk_link", 1, 50.0 / 3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(3, "primary", 1, 80.0 / 3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(3, "primary_link", 1, 60.0 / 3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(4, "secondary", 1, 60.0 / 3.6, 1.0, 1000);
		osmReader.setHighwayDefaults(5, "tertiary", 1, 45.0 / 3.6, 1.0, 600);
		osmReader.setHighwayDefaults(5, "minor", 1, 45.0 / 3.6, 1.0, 600);
		osmReader.setHighwayDefaults(5, "unclassified", 1, 45.0 / 3.6, 1.0, 600);
		osmReader.setHighwayDefaults(6, "residential", 1, 30.0 / 3.6, 1.0, 600);
		osmReader.setHighwayDefaults(6, "living_street", 1, 15.0 / 3.6, 1.0, 300);
		log.info("	Setting parameters for osmReader... done.");

		log.info("	Parsing osmFile...");
		osmReader.parse(osmFile);
		log.info("	Parsing osmFile... done.");

		log.info("Creating street network from osmFile... done.");
		return network;
	}

	/**
	 * Creates a pt network from the given osmFile using playground.scnadine.converters.osmPT.Osm2TransitLines.
	 *
	 * @param osmFile
	 * @return
	 */
	private Network createPTNetwork(String osmFile) {
		log.info("Creating pt network from osmFile...");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);

		Osm2TransitLines osm2pt = new Osm2TransitLines(scenario.getTransitSchedule(), scenario.getNetwork());
		osm2pt.convert(osmFile);

		log.info("Creating pt network from osmFile... done.");
		return scenario.getNetwork();
	}

	/**
	 * Merge the two special networks to a new network. Thereby makes sure that the allowed modes
	 * for each link are merged and not replaced or substituted.
	 * <p/>
	 * The resulting, merged network is written into multimodalNetwork.
	 *
	 * @param multimodalNetwork
	 * @param ptNetwork
	 * @param carNetwork
	 */
	private void mergePTwithCarNetwork(Network multimodalNetwork, Network ptNetwork, Network carNetwork) {
		log.info("Merging pt with car network...");

		// TODO-boescpa Implement mergePTwithCarNetwork...

		// Merge the two special networks to a new network. Thereby makes sure that the allowed modes
		// for each link are merged and not replaced or substituted.
		// The resulting, merged network is written into this.network.

		log.info("	Cleaning created network...");
		new NetworkCleaner().run(this.network);
		log.info("	Cleaning created network... done.");

		log.info("Merging pt with car network... done.");
	}
}
