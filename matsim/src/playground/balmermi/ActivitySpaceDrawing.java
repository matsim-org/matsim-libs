/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySpaceDrawing.java
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

import org.matsim.config.ConfigWriter;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PersonDrawActivtiySpaces;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;

public class ActivitySpaceDrawing {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void drawActivitySpace() {

		System.out.println("calculateActivitySpaces():");

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Population plans = new PopulationImpl(PopulationImpl.USE_STREAMING);
		System.out.println("  done.");

		System.out.println("  adding person algorithms... ");
		plans.addAlgorithm(new PersonDrawActivtiySpaces());
		System.out.println("  done.");

		System.out.println("  creating plans writer object... ");
		PopulationWriter plans_writer = new PopulationWriter(plans);
		plans.addAlgorithm(plans_writer);
		System.out.println("  done.");

		System.out.println("  reading plans, running person-algos and writing the xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans_writer.write();
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
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
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

		Gbl.createConfig(args);

		drawActivitySpace();

		Gbl.printElapsedTime();
	}
}
