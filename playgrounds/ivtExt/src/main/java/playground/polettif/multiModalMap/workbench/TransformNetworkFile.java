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


package playground.polettif.multiModalMap.workbench;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * Transforms a network file .
 *
 * @params 	args[0] inputNetworkFile
 * 			args[1] outputNetworkFile
 * 			args[2] fromSystem
 * 			args[3] toSystem
 *
 * @author polettif
 */
public class TransformNetworkFile {

	private static final Logger log = Logger.getLogger(TransformNetworkFile.class);

	public static void main(String[] args) {

		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(args[2], args[3]);

		// generate basic Config
		Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", args[0]);

		// load scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Network network = scenario.getNetwork();

		NetworkTransform networkTransform = new NetworkTransform(coordinateTransformation);

		networkTransform.run(network);

		NetworkWriter writer = new NetworkWriter(network);
		writer.write(args[1]);

		log.info("Network transformed.");
	}

	public static void run(String inputNetwork, String outputNetwork, String fromSystem, String toSystem) {
		String[] args = {inputNetwork, outputNetwork, fromSystem, toSystem};
		main(args);
	}
	
}