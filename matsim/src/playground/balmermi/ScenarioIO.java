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

import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.Facility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;

import playground.balmermi.modules.PersonFacility2Link;

public class ScenarioIO {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioIO");
		System.out.println();
		System.out.println("Usage1: ScenarioIO configfile");
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

		ScenarioLoader sl = new ScenarioLoader(args[0]);

		System.out.println("loading facilities...");
		sl.loadActivityFacilities();
		Gbl.printMemoryUsage();
		System.out.println("done. (loading facilities)");

		System.out.println("loading network...");
		sl.loadNetwork();
		Gbl.printMemoryUsage();
		System.out.println("done. (loading network)");

		Config config = sl.getScenario().getConfig();
		NetworkLayer network = sl.getScenario().getNetwork();
		ActivityFacilities af = sl.getScenario().getActivityFacilities();

		System.out.println("complete world...");
		Set<String> exTxpes = new TreeSet<String>();
		exTxpes.add("0-4110-0"); // motorway
		exTxpes.add("1-4110-0"); // motorway
		exTxpes.add("2-4130-1"); // ferry
		exTxpes.add("2-4130-2"); // train
		exTxpes.add("3-4130-2"); // train
		exTxpes.add("4-4130-1"); // ferry
		exTxpes.add("4-4130-2"); // train
		exTxpes.add("7-4130-1"); // ferry
		Gbl.getWorld().complete(exTxpes);
		Gbl.printMemoryUsage();
		System.out.println("done. (complete world)");
		
		System.out.println("writing facilities...");
		new FacilitiesWriter(af).write();
		System.out.println("done. (writing facilities)");

		System.out.println("writing network...");
		new NetworkWriter(network).write();
		System.out.println("done. (writing network)");

		System.out.println("loading population...");
		sl.loadPopulation();
		PopulationImpl population = (PopulationImpl)sl.getScenario().getPopulation();
		population.setIsStreaming(false);
		Gbl.printMemoryUsage();
		System.out.println("done. (loading population)");
		
		System.out.println("running algorithms...");
		new PersonFacility2Link().run(population);
		Gbl.printMemoryUsage();
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		PreProcessLandmarks preProcessLandmarks = new PreProcessLandmarks(timeCostCalc);
		preProcessLandmarks.run(network);
		ReRouteLandmarks router = new ReRouteLandmarks(network,timeCostCalc,timeCostCalc,preProcessLandmarks);
		router.prepareReplanning();
		for (PersonImpl person : population.getPersons().values()) {
			for (PlanImpl plan : person.getPlans()) {
				router.handlePlan(plan);
			}
		}
		router.finishReplanning();
		Gbl.printMemoryUsage();
		System.out.println("done. (running algorithms)");

		System.out.println("writing population...");
		new PopulationWriter(population,sl.getScenario().getKnowledges()).write();
		System.out.println("done. (writing population)");
	}
}
