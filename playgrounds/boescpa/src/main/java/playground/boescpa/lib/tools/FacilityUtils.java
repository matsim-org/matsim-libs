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

package playground.boescpa.lib.tools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

/**
 * Provides different useful static methods around facilities...
 *
 * @author boescpa
 */
public class FacilityUtils {

	/**
	 * Directly loads and provides a network given a path to a network file.
	 */
	public static ActivityFacilities readFacilities(String path2Facilities) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
		reader.readFile(path2Facilities);
		return scenario.getActivityFacilities();
	}

}
