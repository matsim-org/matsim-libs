/* *********************************************************************** *
 * project: org.matsim.*
 * MakeScenario.java
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

package playground.jhackney;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.algorithms.PersonSetActToLinkWithNonNullFacility;

public class MakeScenario {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run(Config config) {

		System.out.println("Make Scenario SAMPLE OF FACILITIES:");

		ScenarioImpl scenario = new ScenarioImpl(config);

		System.out.println("Uses output of a CUT. Samples 100x\"pct\"% of the facilities and moves Acts to take place at these");

//		System.out.println("  reading world xml file... ");
//		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
//		worldReader.readFile(config.world().getInputFile());
//		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network xml file...");
//		NetworkLayer network = null;
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		System.out.println("  done.");

		//		System.out.println("  reading matrices xml file... ");
//		MatsimMatricesReader reader = new MatsimMatricesReader(Matrices.getSingleton());
//		reader.readFile(config.matrices().getInputFile());
//		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationImpl plans = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		// ch.cut.640000.200000.740000.310000.xml
//		CoordI min = new Coord(640000.0,200000.0);
//		CoordI max = new Coord(740000.0,310000.0);
//
		System.out.println("  running plans modules... ");
//		new PersonRemoveReferences().run(plans);
//		new PlansScenarioCut(min,max).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		System.out.println("  Completing World ... ");
		new WorldConnectLocations().run(scenario.getWorld());
		System.out.println("  done.");
		//////////////////////////////////////////////////////////////////////

		System.out.println("  running facilities modules... ");
////		new FacilitiesSetCapacity().run(facilities);
////		new FacilitiesScenarioCut(min,max).run(facilities);
//		double pct=0.01;
//		new FacilitiesMakeSample(pct).run(facilities);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  running network modules... ");
//		network.addAlgorithm(new NetworkSummary());
//		new NetworkScenarioCut(min,max).run(network);
//		network.addAlgorithm(new NetworkSummary());
//		network.addAlgorithm(new NetworkCleaner(false));
//		network.addAlgorithm(new NetworkSummary());
//		NetworkWriteAsTable nwat = new NetworkWriteAsTable();
//		network.addAlgorithm(nwat);
//		network.runAlgorithms();
//		nwat.close();
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running plans modules... ");

		new PersonSetActToLinkWithNonNullFacility(facilities).run(plans);
//    	new XY2Links(network).run(plans);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		new PlansCalcRoute(config.plansCalcRoute(), network,timeCostCalc,timeCostCalc).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  running matrices algos... ");
//		new MatricesCompleteBasedOnFacilities(facilities, (ZoneLayer)world.getLayer("municipality")).run(Matrices.getSingleton());
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		Scenario.writePlans(plans, network);
		Scenario.writeNetwork(network);
		Scenario.writeFacilities(facilities);
		Scenario.writeWorld(scenario.getWorld());
		Scenario.writeConfig();

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		run(config);

		Gbl.printElapsedTime();
	}
}
