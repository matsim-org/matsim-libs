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

import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
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
		System.out.println("Usage1: PopulationCreation inputFacilitiesFile datapulsPersonsFile outputPlansFile");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		if (args.length != 3) { printUsage(); return; }

		System.out.println("loading facilities...");
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(args[0]);
		Gbl.printMemoryUsage();
		System.out.println("done. (loading facilities)");

		System.out.println("creating population...");
		PopulationImpl population = new PopulationImpl();
		Knowledges knowledges = new KnowledgesImpl();
		Gbl.printMemoryUsage();
		System.out.println("done. (loading population)");

		System.out.println("running modules...");
		new PlansCreateFromDataPuls(args[1],facilities,knowledges).run(population);
		Gbl.printMemoryUsage();
		System.out.println("done. (running modules)");

		System.out.println("writing population...");
		new PopulationWriter(population,knowledges).writeFile(args[2]);
		System.out.println("done. (writing population)");
	}
}
