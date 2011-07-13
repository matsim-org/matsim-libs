/* *********************************************************************** *
 * project: org.matsim.*
 * CorrectDataFile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.possiblesharedrides;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Corrects a data file for which distances were got from route
 * @author thibautd
 */
public class CorrectDataFile {
	public static void main(final String[] args) {
		String configFile = args[0];
		String dataFile = args[1];
		String outputFile = args[2];

		CountPossibleSharedRideNew counter = new CountPossibleSharedRideNew(0,0);
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));

		counter.correctFileFromPlans(dataFile, scenario.getPopulation(), scenario.getNetwork(), outputFile);
	}
}

