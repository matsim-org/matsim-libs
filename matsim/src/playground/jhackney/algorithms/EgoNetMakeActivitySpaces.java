/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
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

package playground.jhackney.algorithms;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonCalcActivitySpace;
import org.matsim.plans.algorithms.PersonDrawActivtiySpaces;
import org.matsim.plans.algorithms.PersonWriteActivitySpaceTable;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.Scenario;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;

public class EgoNetMakeActivitySpaces {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make activity spaces for egoNet:");

		Scenario.setUpScenarioConfig();

		Scenario.readWorld();
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();
		Plans plans = Scenario.readPlans();
		//read in social network
		System.out.println(" Initializing the social network ...");
		new SocialNetwork(plans);
		System.out.println("... done");
		
		//read in facilities knowledge
		new InitializeKnowledge(plans);
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
		//////////////////////////////////////////////////////////////////////

//		// ch.cut.640000.200000.740000.310000.xml
//		CoordI min = new Coord(640000.0,200000.0);
//		CoordI max = new Coord(740000.0,310000.0);

//		System.out.println("  running plans modules... ");
//		new PersonRemoveReferences().run(plans);
//		new PlansScenarioCut(min,max).run(plans);
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  running facilities modules... ");
////		new FacilitiesSetCapacity().run(facilities);
//		new FacilitiesScenarioCut(min,max).run(facilities);
//		System.out.println("  done.");

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
//
//		System.out.println("  running world modules... ");
//		new WorldCreateRasterLayer(3000).run(Gbl.getWorld());
//		new WorldCheck().run(Gbl.getWorld());
//		new WorldBottom2TopCompletion().run(Gbl.getWorld());
//		new WorldValidation().run(Gbl.getWorld());
//		new WorldCheck().run(Gbl.getWorld());
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running plans modules... ");
//		new PersonAssignLinkViaFacility(network,facilities).run(plans);
////		new XY2Links(network).run(plans);
//		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
//		new PlansCalcRoute(network,timeCostCalc,timeCostCalc).run(plans);
//		new PersonsRandomizeId(plans);
		Person ego=plans.getPerson("21924270");

		Plans socialPlans = new PersonGetEgoNetGetPlans().extract(ego, plans);
//		// make the set of plans to use as EgoNet
		socialPlans.addAlgorithm(new PersonCalcActivitySpace("all"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("leisure"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("work"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("home"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("shop"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("education"));
		PersonWriteActivitySpaceTable pwast = new PersonWriteActivitySpaceTable();
		socialPlans.addAlgorithm(pwast);
		socialPlans.addAlgorithm(new PersonDrawActivitySpace());
		System.out.println("  done.");

		
		System.out.println("  Initializing the KML output");
//		this.kmlOut=new EgoNetPlansMakeKML(this.controler.getConfig());
//		EgoNetPlansMakeKML.setUp(this.controler.getConfig(), this.controler.getNetwork());
//		EgoNetPlansMakeKML.generateStyles();

		System.out.println("... done");
		
		
		//////////////////////////////////////////////////////////////////////

//		System.out.println("  running matrices algos... ");
//		new MatricesCompleteBasedOnFacilities(facilities, (ZoneLayer)Gbl.getWorld().getLayer("municipality")).run(Matrices.getSingleton());
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		System.out.println("  finishing person algorithms...");
		socialPlans.runAlgorithms();
		pwast.close();
		System.out.println("  done.");
		
		Scenario.writePlans(socialPlans);
//		Scenario.writeNetwork(network);
//		Scenario.writeFacilities(facilities);
//		Scenario.writeWorld(Gbl.getWorld());
//		Scenario.writeConfig();

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();

		run();

		Gbl.printElapsedTime();
	}
}
