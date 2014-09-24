/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.johannes.gsv.synPop.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author johannes
 *
 */
public class OSM2Network {

	private static final Logger logger = Logger.getLogger(OSM2Network.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		
		CoordinateTransformation transformation = new GeotoolsTransformation("EPSG:4326", "EPSG:31467");
		OsmNetworkReader reader = new OsmNetworkReader(network, transformation);
		
		logger.info("Loading OSM file...");
		reader.setHierarchyLayer(60, 0, 40, 20, 5);
		reader.setMemoryOptimization(true);
		reader.setKeepPaths(true);
		reader.parse(args[0]);

		logger.info("Writing network file...");
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(args[1]);
		logger.info("Done.");
	}

}
