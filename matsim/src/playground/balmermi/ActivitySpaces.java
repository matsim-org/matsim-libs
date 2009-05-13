/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySpaces.java
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

import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.PersonCalcActivitySpace;
import org.matsim.population.algorithms.PersonDrawActivtiySpaces;
import org.matsim.population.algorithms.PersonWriteActivitySpaceTable;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

public class ActivitySpaces {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void calculateActivitySpaces(Config config) {

		System.out.println("calculateActivitySpaces():");

		World world = Gbl.createWorld();
		
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		ActivityFacilities facilities = (ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		System.out.println("  done.");

		System.out.println("  adding person algorithms... ");

//		plans.addAlgorithm(new PersonCalcActivitySpace("all"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("leisure"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("work"));
		plans.addAlgorithm(new PersonCalcActivitySpace("home"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("shop"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("education"));
		PersonWriteActivitySpaceTable pwast = new PersonWriteActivitySpaceTable();
		plans.addAlgorithm(pwast);
		plans.addAlgorithm(new PersonDrawActivtiySpaces());
		System.out.println("  done.");

		System.out.println("  creating plans writer object... ");
		PopulationWriter plans_writer = new PopulationWriter(plans);
		plans.addAlgorithm(plans_writer);
		System.out.println("  done.");

		System.out.println("  reading plans, running person-algos and writing the xml file... ");
		new MatsimPopulationReader(plans, null).readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  finishing person algorithms...");
		pwast.close();
		System.out.println("  done.");

		// writing all available input

		System.out.println("  writing plans xml file... ");
		plans_writer.write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilities);
		facilities_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(config);
		config_writer.write();
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

//		if (!args[0].endsWith("test/balmermi/ActivitySpaces_config.xml")) {
//			Gbl.errorMsg(ActivitySpaces.class,"main(String[] args)",
//			             "This run must be based on 'test/balmermi/" +
//			             "ActivitySpaces_config.xml' input config file.");
//		}

		Config config = Gbl.createConfig(args);

		calculateActivitySpaces(config);

		Gbl.printElapsedTime();
	}
}
