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

import org.matsim.gbl.Gbl;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.world.MatsimWorldReader;

import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Persons;
import playground.ciarif.models.subtours.PersonInitDemandSummaryTable;
import playground.ciarif.models.subtours.PersonModeChoiceModel;

public class PersonSubtoursStreaming {

	public static void run() {

		System.out.println("Run...");

		System.out.println("  reading world xml file... ");
		new MatsimWorldReader(Gbl.getWorld()).readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/ gg25_2001_infos.txt");
		municipalities.parse();
		System.out.println("  done.");

		System.out.println("  parsing household information... ");
		Households households = new Households(municipalities,"input/ households2000.txt");
		households.parse();
		System.out.println("  done.");

		System.out.println("  parsing person information... ");
		Persons persons = new Persons(households,"input/ persons2000.txt");
		persons.parse();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Population plans = new Population(Population.USE_STREAMING);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  adding person modules... ");
//		Household hh = new Household(0,null);
//		Person person = new Person(0,hh);
//		Households hhs = new Households(null,null);
//		hhs.setHH(hh);
//		Persons persons = new Persons(hhs,null);
//		persons.households = hhs;
//		persons.persons.put(person.p_id,person);
		
		System.out.println("  adding mode choice module...");
		PersonModeChoiceModel pmcm= new PersonModeChoiceModel(persons, municipalities);
		plans.addAlgorithm(pmcm);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("  writing summary table...");
		PersonInitDemandSummaryTable pidst = new PersonInitDemandSummaryTable("output/output_persons.txt", pmcm.getPersonSubtours());
		pidst.write();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();
		
		Gbl.createConfig(args);
		run();

		Gbl.printElapsedTime();
	}
}
