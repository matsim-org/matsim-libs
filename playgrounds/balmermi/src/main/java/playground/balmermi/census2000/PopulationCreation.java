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

import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;

import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Persons;
import playground.balmermi.census2000.modules.PlansCreatePopulation;
import playground.balmermi.world.World;

public class PopulationCreation {

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createPopulation(Config config) {

		System.out.println("MATSim-POP: create Population based on census2000 data.");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		World world = new World();


		System.out.println("  reading facilities xml file... ");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		Matrices matrices = new Matrices();
		MatsimMatricesReader reader = new MatsimMatricesReader(matrices, scenario);
		reader.readFile(null /*filename not specified*/);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/gg25_2001_infos.txt");
		municipalities.parse(world.getLayer(new IdImpl("municipality")));
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
		Population plans = scenario.getPopulation();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running plans module... ");
		new PlansCreatePopulation(persons).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, null).write(null);//config.plans().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing matrices xml file... ");
		MatricesWriter mat_writer = new MatricesWriter(matrices);
		mat_writer.write(null /*filename not specified*/);
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(null /* filename not specified */);
		System.out.println("  done.");

//		System.out.println("  writing world xml file... ");
//		WorldWriter world_writer = new WorldWriter(world);
//		world_writer.write(null);//config.world().getOutputFile());
//		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {

		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);
		createPopulation(config);

		Gbl.printElapsedTime();
	}
}
