/* *********************************************************************** *
 * project: org.matsim.*
 * PersonStreaming.java
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

package playground.ciarif;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;

import playground.balmermi.census2000.data.Household;
import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Person;
import playground.balmermi.census2000.data.Persons;
import playground.ciarif.models.subtours.PersonInitDemandSummaryTable;
import playground.ciarif.models.subtours.PersonModeChoiceModel;

public class PersonStreaming {

	public static void run() {

		//Config config = Gbl.createConfig(args);
		Scenario.setUpScenarioConfig();
		Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		
		//////////////////////////////////////////////////////////////////////
		
		
		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Population plans = new Population(Population.USE_STREAMING);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		//SubtoursWriteTable subtoursWriteTable = new SubtoursWriteTable ("output/output_persons_subtours.txt");
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		Household hh = new Household(0,null);
//		hh.coord = new Coord(100,100);
		Person person = new Person(0,hh);
//		//person.age = 30;
//		//person.car_avail = "always";
//		//person.license = true;
//		//person.male = true;
		Households hhs = new Households(null,null);
//		hhs.setHH(hh);
		Persons persons = new Persons(hhs,null);
//		persons.households = hhs;
		persons.persons.put(person.p_id,person);
		
		//PersonModeChoiceModel pmcm = new PersonModeChoiceModel(persons);
		//plans.addAlgorithm(pmcm);
		
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		//plansReader.readFile ("input/output_plans.xml");
		plans.printPlansCount();
		//PersonInitDemandSummaryTable pidst = new PersonInitDemandSummaryTable("output/output_persons.txt", pmcm.getPersonSubtours());
		//pidst.write();
		plansWriter.write();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  finishing algorithms... ");
		System.out.println("  done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();
		run();
		Gbl.printElapsedTime();
	}
}
