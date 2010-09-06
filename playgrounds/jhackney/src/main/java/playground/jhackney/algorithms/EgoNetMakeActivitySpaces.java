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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.Scenario;
import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.activitySpaces.PersonCalcActivitySpace;
import playground.jhackney.activitySpaces.PersonWriteActivitySpaceTable;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;

public class EgoNetMakeActivitySpaces {


	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make activity spaces for egoNet:");

		SocNetConfigGroup snConfig = Scenario.setUpScenarioConfig();
		Config config = Scenario.getConfig();

		World world = Scenario.readWorld();
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();

		System.out.println("  reading plans xml file... ");
		System.out.println(config.plans().getInputFile());
		new MatsimPopulationReader(Scenario.getScenarioImpl()).readFile(config.plans().getInputFile());
		Population plans = Scenario.getScenarioImpl().getPopulation();
		Knowledges knowledges = Scenario.getScenarioImpl().getKnowledges();
		System.out.println("  done.");

		ActivityFacilitiesImpl facilities = Scenario.readFacilities();
		//read in social network
		System.out.println(" Initializing the social network ...");
		new SocialNetwork(plans, facilities, snConfig);
		System.out.println("... done");

		//read in facilities knowledge
		new InitializeKnowledge(plans, facilities,  knowledges, network, snConfig);
		new WorldConnectLocations(config).run(world);
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
		Person ego=plans.getPersons().get(new IdImpl("21924270"));

		PopulationImpl socialPlans = (PopulationImpl) new PersonGetEgoNetGetPlans().extract(ego);
//		// make the set of plans to use as EgoNet
		socialPlans.addAlgorithm(new PersonCalcActivitySpace("all", knowledges));
//		plans.addAlgorithm(new PersonCalcActivitySpace("leisure"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("work"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("home"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("shop"));
//		plans.addAlgorithm(new PersonCalcActivitySpace("education"));
		PersonWriteActivitySpaceTable pwast = new PersonWriteActivitySpaceTable(knowledges);
		socialPlans.addAlgorithm(pwast);
		socialPlans.addAlgorithm(new PersonDrawActivitySpace(knowledges));
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

		Scenario.writePlans(socialPlans, network, "outputPlans.xml");
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
