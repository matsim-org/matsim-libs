/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiyParsing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi;

import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.world.World;

import playground.balmermi.census2000.modules.FacilitiesSetCapacity;

public class FacilitiyParsing {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void testRun01(Config config) {

		System.out.println("TEST RUN 01:");

		World world = Gbl.createWorld();
		
		System.out.println("  reading facilities xml file... ");
		ActivityFacilities facilities = (ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  running facilities algorithms... ");
//		Facilities.getSingleton().addAlgorithm(new FacilitiesSummary());
//		Facilities.getSingleton().addAlgorithm(new FacilitiesSpatialCut());
		new FacilitiesSetCapacity().run(facilities);
//		facilities.runAlgorithms();
		System.out.println("  done.");
		
		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write();
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

		Gbl.startMeasurement();

		Config config = new ScenarioLoader(args[0]).getScenario().getConfig();

		testRun01(config);

		Gbl.printElapsedTime();
	}
}
