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


package playground.polettif.publicTransitMapping.tools;

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
 * Performs a coordinate transformation on a network file .
 *
 * @author polettif
 */
public class TransformNetworkFile {

	private static final Logger log = Logger.getLogger(TransformNetworkFile.class);

	/**
	 * 	Transforms a network file.
	 *
	 * @param 	args <br/>
	 *          args[0] inputNetworkFile<br/>
	 * 			args[1] outputNetworkFile<br/>
	 * 			args[2] fromSystem<br/>
	 * 			args[3] toSystem
	 */
	public static void main(String[] args) {
			run(args[0], args[1], args[2], args[3]);
		}

	/**
	 * 	Transforms a network file.
	 */
	public static void run(String inputNetwork, String outputNetwork, String fromSystem, String toSystem) {

		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(fromSystem, toSystem);

		Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", inputNetwork);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		NetworkTransform networkTransform = new NetworkTransform(coordinateTransformation);
		networkTransform.run(network);

		new NetworkWriter(network).write(outputNetwork);

		log.info("Network transformed.");
	}

}