/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationCreation.java
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

import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;

import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Persons;
import playground.balmermi.census2000.modules.PlansCreatePopulation;

public class PopulationCreation {

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createPopulation() {

		System.out.println("MATSim-POP: create Population based on census2000 data.");

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		MatsimMatricesReader reader = new MatsimMatricesReader(Matrices.getSingleton());
		reader.readFile(Gbl.getConfig().matrices().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/gg25_2001_infos.txt");
		municipalities.parse();
		System.out.println("  done.");

		System.out.println("  parsing household information... ");
		Households households = new Households(municipalities,"input/households2000.txt");
		households.parse();
		System.out.println("  done.");

		System.out.println("  parsing person information... ");
		Persons persons = new Persons(households,"input/persons2000.txt");
		persons.parse();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Plans plans = new Plans(Plans.NO_STREAMING);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running plans module... ");
		new PlansCreatePopulation(persons).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing plans xml file... ");
		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("  done.");

		System.out.println("  writing matrices xml file... ");
		MatricesWriter mat_writer = new MatricesWriter(Matrices.getSingleton());
		mat_writer.write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		createPopulation();

		Gbl.printElapsedTime();
	}
}
