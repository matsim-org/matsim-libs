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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

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

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		final Population population = (Population) scenario.getPopulation();
		StreamingPopulationReader population = new StreamingPopulationReader( scenario ) ;
		StreamingDeprecated.setIsStreaming(population, true);
		MatsimReader plansReader = new PopulationReader(scenario);
		StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		plansWriter.startStreaming(args[1].trim());

		ActivityFacilities afs = scenario.getActivityFacilities();
		
		System.out.println("adding algorithms...");
		population.addAlgorithm(new PersonAdaptPlanAndCreateFacilities(afs));
		final PersonAlgorithm algo = plansWriter;
		population.addAlgorithm(algo);
		System.out.println("done. (adding algorithms)");

		System.out.println("stream population...");
		plansReader.readFile(args[0].trim());
		PopulationUtils.printPlansCount(population) ;
		plansWriter.closeStreaming();
		Gbl.printMemoryUsage();
		System.out.println("done. (stream population)");

		System.out.println("writing facilities...");
		new FacilitiesWriter(afs).write(args[2].trim());
		System.out.println("done. (writing facilities)");
	}
}
