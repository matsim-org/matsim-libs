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
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;

import playground.balmermi.census2000.data.Household;
import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Person;
import playground.balmermi.census2000.data.Persons;
import playground.ciarif.models.subtours.PersonInitDemandSummaryTable;
import playground.ciarif.models.subtours.PersonModeChoiceModel;

public class PersonSubtoursStreaming {

	public static void run() {

		Scenario.setUpScenarioConfig();

		System.out.println("person streaming...");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Plans plans = new Plans(Plans.USE_STREAMING);
		PlansWriter plansWriter = new PlansWriter(plans);
		//SubtoursWriteTable subtoursWriteTable = new SubtoursWriteTable ("output/output_persons_subtours.txt");
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		Household hh = new Household(0,null);
		Person person = new Person(0,hh);
		Households hhs = new Households(null,null);
		hhs.setHH(hh);
		Persons persons = new Persons(hhs,null);
		persons.households = hhs;
		persons.persons.put(person.p_id,person);
		
		PersonModeChoiceModel pmcm= new PersonModeChoiceModel(persons);
		plans.addAlgorithm(pmcm);
		
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		PersonInitDemandSummaryTable pidst = new PersonInitDemandSummaryTable("output/output_persons.txt", pmcm.getPersonSubtours());
		pidst.write();
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
