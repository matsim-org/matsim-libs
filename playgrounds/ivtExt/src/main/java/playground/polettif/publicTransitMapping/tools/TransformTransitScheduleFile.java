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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;


/**
 * Transforms a MATSim Transit Schedule file.
 *
 * @author polettif
 */
public class TransformTransitScheduleFile {

	private static final Logger log = Logger.getLogger(TransformTransitScheduleFile.class);

	/**
	 * Transforms a MATSim Transit Schedule file.
	 *
	 * @param args <br/>
	 *             args[0] inputScheduleFile<br/>
	 *             args[1] outputScheduleFile<br/>
	 *             args[2] fromCoordinateSystem<br/>
	 *             args[3] toCoordinateSystem
	 */
	public static void main(String[] args) {
		run(args[0], args[1], args[2], args[3]);
	}

	/**
	 * Transforms a MATSim Transit Schedule file.
	 */
	public static void run(String inputSchedule, String outputSchedule, String fromCoordinateSystem, String toCoordinateSystem) {
		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(fromCoordinateSystem, toCoordinateSystem);
		Config config = ConfigUtils.createConfig();

		// load scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		new TransitScheduleReader(coordinateTransformation, scenario).readFile(inputSchedule);
		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleWriter(schedule).writeFile(outputSchedule);

		log.info("Schedule transformed.");
	}
	
}