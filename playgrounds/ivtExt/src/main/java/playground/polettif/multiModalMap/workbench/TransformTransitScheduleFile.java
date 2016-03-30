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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;


/**
 * Transforms a network file .
 *
 * @params 	args[0] inputScheduleFile
 * 			args[1] outpuScheduleFile
 * 			args[2] fromSystem
 * 			args[3] toSystem
 *
 * @author polettif
 */
public class TransformTransitScheduleFile {

	private static final Logger log = Logger.getLogger(TransformTransitScheduleFile.class);
	private static TransitSchedule schedule;

	public static void main(String[] args) {

		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(args[2], args[3]);

		// generate basic Config
		Config config = ConfigUtils.createConfig();

		// load scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		new TransitScheduleReader(coordinateTransformation, scenario).readFile(args[0]);
		schedule = scenario.getTransitSchedule();

		new TransitScheduleWriter(schedule).writeFile(args[1]);

		log.info("Schedule transformed.");
	}

	public static void run(String inputNetwork, String outputNetwork, String fromSystem, String toSystem) {
		String[] args = {inputNetwork, outputNetwork, fromSystem, toSystem};
		main(args);
	}
	
}