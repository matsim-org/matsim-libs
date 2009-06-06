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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;

public class ScenarioIO {

	//////////////////////////////////////////////////////////////////////

	private static void facility2link(Scenario scenario) {
		System.out.println("adding link ids to the plans from facilities...");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Activity act = plan.getFirstActivity();
				while (act != plan.getLastActivity()) {
					act.setLink(act.getFacility().getLink());
					act = plan.getNextActivity(plan.getNextLeg(act));
				}
				act.setLink(act.getFacility().getLink());
			}
		}
		
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	private static void initRoutes(Scenario scenario) {
		System.out.println("calc initial routes...");
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		new PlansCalcRoute(scenario.getConfig().plansCalcRoute(),scenario.getNetwork(),timeCostCalc,timeCostCalc,new AStarLandmarksFactory(scenario.getNetwork(),timeCostCalc)).run(scenario.getPopulation());
		System.out.println("done.");
	}

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
		Scenario scenario = new ScenarioLoader(args[0]).loadScenario();
		Gbl.printMemoryUsage();
		facility2link(scenario);
		Gbl.printMemoryUsage();
		initRoutes(scenario);
		Gbl.printMemoryUsage();
		new FacilitiesWriter(scenario.getActivityFacilities()).write();
		Gbl.printMemoryUsage();
		new NetworkWriter(scenario.getNetwork()).write();
		Gbl.printMemoryUsage();
		new PopulationWriter(scenario.getPopulation()).write();
		Gbl.printMemoryUsage();
	}
}
