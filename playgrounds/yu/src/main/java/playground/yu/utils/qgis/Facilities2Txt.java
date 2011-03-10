/* *********************************************************************** *
 * project: org.matsim.*
 * Facilities2Txt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils.qgis;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.utils.io.SimpleWriter;

/**
 * extracts informations from facilities-file and makes a txt-file which can be
 * added in QGIS file.
 * 
 * @author yu
 */
public class Facilities2Txt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String //
		// netFilename = "../matsim/test/scenarios/chessboard/network.xml", //
		// popFilename = "../matsim/test/scenarios/chessboard/plans.xml", //
		facilitiesFilename = "../matsim/test/scenarios/chessboard/facilities.xml", // 
		facilitiesTxtFilename = "../matsimTests/locationChoice/chessboard/facilities.txt";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimFacilitiesReader((ScenarioImpl) scenario)
				.readFile(facilitiesFilename);

		SimpleWriter writer = new SimpleWriter(facilitiesTxtFilename);
		writer.writeln("Id\tx\ty\tacts\tcapacities");

		ActivityFacilities facilities = scenario.getActivityFacilities();
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Coord coord = facility.getCoord();
			writer.write(facility.getId() + "\t" + coord.getX() + "\t"
					+ coord.getY() + "\t");

			Map<String, ? extends ActivityOption> activityOptions = facility
					.getActivityOptions();
			// acts
			writer.write(activityOptions.keySet());
			// capacities
			for (ActivityOption activityOption : activityOptions.values()) {
				writer.write("\t" + activityOption.getCapacity());
			}
			writer.writeln();
			writer.flush();
		}

		writer.close();
	}
}
