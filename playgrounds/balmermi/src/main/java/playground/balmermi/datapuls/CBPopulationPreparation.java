/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCreation.java
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

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.balmermi.datapuls.modules.PersonAdaptPlanAndCreateFacilities;

public class CBPopulationPreparation {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("FacilitiesCreation");
		System.out.println();
		System.out.println("Usage: CBPopulationPreparation inputCBPopulation outputPopulation outputFacilities");
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

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		PopulationWriter plansWriter = new PopulationWriter(population, scenario.getNetwork());
		plansWriter.startStreaming(args[1].trim());

		ActivityFacilitiesImpl afs = scenario.getActivityFacilities();

		System.out.println("adding algorithms...");
		population.addAlgorithm(new PersonAdaptPlanAndCreateFacilities(afs));
		population.addAlgorithm(plansWriter);
		System.out.println("done. (adding algorithms)");

		System.out.println("stream population...");
		plansReader.readFile(args[0].trim());
		population.printPlansCount();
		plansWriter.closeStreaming();
		Gbl.printMemoryUsage();
		System.out.println("done. (stream population)");

		System.out.println("writing facilities...");
		new FacilitiesWriter(afs).write(args[2].trim());
		System.out.println("done. (writing facilities)");
	}
}
