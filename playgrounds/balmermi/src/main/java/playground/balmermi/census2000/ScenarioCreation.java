/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioCreation.java
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

package playground.balmermi.census2000;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;
import org.matsim.world.ZoneLayer;

import playground.balmermi.census2000.modules.FacilitiesSetCapacity;
import playground.balmermi.census2000.modules.MatricesCompleteBasedOnFacilities;

public class ScenarioCreation {

	//////////////////////////////////////////////////////////////////////
	// fuseScenario()
	//////////////////////////////////////////////////////////////////////

	public static void fuseScenario(Config config) {

		System.out.println("MATSim-FUSION: Consolidate World, Facilities and Matrices.");

		ScenarioImpl scenario = new ScenarioImpl(config);
		
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		Matrices matrices = new Matrices();
		MatsimMatricesReader reader = new MatsimMatricesReader(matrices, scenario);
		reader.readFile(config.matrices().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running facilities modules... ");
		new FacilitiesSetCapacity().run(facilities);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running matrices modules... ");
		new MatricesCompleteBasedOnFacilities(facilities, (ZoneLayer)scenario.getWorld().getLayer("municipality")).run(matrices);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing matrices xml file... ");
		MatricesWriter mat_writer = new MatricesWriter(matrices);
		mat_writer.write(config.matrices().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(config.facilities().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(scenario.getWorld());
		world_writer.write(config.world().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		new ConfigWriter(config).write(config.config().getOutputFile());
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		fuseScenario(config);

		Gbl.printElapsedTime();
	}
}
