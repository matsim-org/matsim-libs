/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioIO.java
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

package playground.balmermi;

import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;

import playground.balmermi.modules.PersonFacility2Link;

public class ScenarioIO {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioIO");
		System.out.println();
		System.out.println("Usage1: ScenarioCut configfile");
		System.out.println("        add a MATSim config file as the only input parameter.");
		System.out.println();
		System.out.println("Note: config file should at least contain the following parameters:");
		System.out.println("      inputNetworkFile");
		System.out.println("      outputNetworkFile");
		System.out.println("      inputFacilitiesFile");
		System.out.println("      outputFacilitiesFile");
		System.out.println("      inputPlansFile");
		System.out.println("      outputPlansFile");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		if (args.length != 1) { printUsage(); return; }
		Gbl.printMemoryUsage();
		ScenarioLoader sl = new ScenarioLoader(args[0]);
		Gbl.printMemoryUsage();
		sl.loadActivityFacilities();
		Gbl.printMemoryUsage();
		sl.loadNetwork();
		Gbl.printMemoryUsage();
		Gbl.getWorld().complete();

		Config config = sl.getScenario().getConfig();
		Network network = sl.getScenario().getNetwork();

		final PopulationImpl population = (PopulationImpl)sl.getScenario().getPopulation();
		population.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(population,network);
		PopulationWriter plansWriter = new PopulationWriter(population);

		Gbl.printMemoryUsage();
		population.addAlgorithm(new PersonFacility2Link());
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		population.addAlgorithm(new PlansCalcRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, new AStarLandmarksFactory(network, timeCostCalc)));
		population.addAlgorithm(plansWriter);
		Gbl.printMemoryUsage();

		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		plansWriter.write();
		Gbl.printMemoryUsage();
	}
}
