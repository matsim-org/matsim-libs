/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationCreation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.datapuls;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;

import playground.balmermi.datapuls.modules.PlansCreateFromDataPuls;

public class PopulationCreation {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("PopulationCreation");
		System.out.println();
		System.out.println("Usage1: PopulationCreation censusConfig inputDataPulsFacilitiesFile datapulsPersonsFile outputPlansFile");
		System.out.println();
		System.out.println("Note: config file should at least contain the following parameters:");
		System.out.println("      inputFacilitiesFile");
		System.out.println("      inputPlansFile");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		if (args.length != 4) { printUsage(); return; }

		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);

		System.out.println("loading census facilities...");
		sl.loadActivityFacilities();
		Gbl.printMemoryUsage();
		System.out.println("done. (census facilities)");

		System.out.println("loading census population...");
		sl.loadPopulation();
		Gbl.printMemoryUsage();
		System.out.println("done. (census population)");

		System.out.println("loading datapuls facilities...");
		ScenarioImpl datapulsScenario = new ScenarioImpl();
		ActivityFacilitiesImpl datapulsFacilities = datapulsScenario.getActivityFacilities();
		new MatsimFacilitiesReader(datapulsScenario).readFile(args[1]);
		Gbl.printMemoryUsage();
		System.out.println("done. (loading datapuls facilities).");

		System.out.println("creating datapuls population...");
		PopulationImpl datapulsPopulation = new ScenarioImpl().getPopulation();
		Knowledges datapulsKnowledges = new KnowledgesImpl();
		Gbl.printMemoryUsage();
		System.out.println("done. (creating datapuls population)");

		System.out.println("running modules...");
		new PlansCreateFromDataPuls(args[2],sl.getScenario(),datapulsFacilities).run(datapulsPopulation,datapulsKnowledges);
		Gbl.printMemoryUsage();
		System.out.println("done. (running modules)");

		System.out.println("writing population...");
		new PopulationWriter(datapulsPopulation,sl.getScenario().getNetwork(), datapulsKnowledges).writeFile(args[3]);
		System.out.println("done. (writing population)");
	}
}
