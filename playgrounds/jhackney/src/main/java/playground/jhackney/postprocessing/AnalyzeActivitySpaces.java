package playground.jhackney.postprocessing;
/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeSocialNets.java
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

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.Scenario;
import playground.jhackney.algorithms.PersonCalcASD2;

public class AnalyzeActivitySpaces {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make activity spaces for egoNet:");


		Scenario.setUpScenarioConfig();
		Config config = Scenario.getConfig();

		World world = Scenario.readWorld(null /*filename not specified*/);
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();
		new WorldConnectLocations(config).run(world);

		Population plans = Scenario.readPlans();
			//read in social network
//		System.out.println(" Initializing the social network ...");
//		SocialNetwork snet=new SocialNetwork(plans);
//
//		System.out.println("... done");

			//read in facilities knowledge
//		config.socnetmodule().setReadMentalMap("true");
//		new InitializeKnowledge(plans);

			//////////////////////////////////////////////////////////////////////

//			// ch.cut.640000.200000.740000.310000.xml
//			CoordI min = new Coord(640000.0,200000.0);
//			CoordI max = new Coord(740000.0,310000.0);

//			System.out.println("  running plans modules... ");
//			new PersonRemoveReferences().run(plans);
//			new PlansScenarioCut(min,max).run(plans);
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running facilities modules... ");
////			new FacilitiesSetCapacity().run(facilities);
//			new FacilitiesScenarioCut(min,max).run(facilities);
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running network modules... ");
//			network.addAlgorithm(new NetworkSummary());
//			new NetworkScenarioCut(min,max).run(network);
//			network.addAlgorithm(new NetworkSummary());
//			network.addAlgorithm(new NetworkCleaner(false));
//			network.addAlgorithm(new NetworkSummary());
//			NetworkWriteAsTable nwat = new NetworkWriteAsTable();
//			network.addAlgorithm(nwat);
//			network.runAlgorithms();
//			nwat.close();
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running world modules... ");
//			new WorldCreateRasterLayer(3000).run(Gbl.getWorld());
//			new WorldCheck().run(Gbl.getWorld());
//			new WorldBottom2TopCompletion().run(Gbl.getWorld());
//			new WorldValidation().run(Gbl.getWorld());
//			new WorldCheck().run(Gbl.getWorld());
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running plans modules... ");
//			new PersonAssignLinkViaFacility(network,facilities).run(plans);
////			new XY2Links(network).run(plans);
//			FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
//			new PlansCalcRoute(network,timeCostCalc,timeCostCalc).run(plans);
//			new PersonsRandomizeId(plans);

//			PlansPlotScoreDistance ppsd=new PlansPlotScoreDistance(plans);
//			ppsd.run(plans);
//			ppsd.plot(Gbl.getConfig().socnetmodule().getOutDir(), "TRB5");


//		Person ego=plans.getPerson("21924270");//1pct
////		Person ego=this.controler.getPopulation().getPerson("21462061");//10pct
//
////			Write out the KML for the EgoNet of a chosen agent
//			System.out.println("  Initializing the KML output");
//
//			EgoNetPlansItersMakeKML.setUp(Scenario.getConfig(), network);
//			EgoNetPlansItersMakeKML.generateStyles();
//
//			System.out.println("... done");
//
//			System.out.println(" Writing out KMZ activity spaces and day plans for agent's egoNet");
//
//			EgoNetPlansItersMakeKML.loadData(ego,500);
//			EgoNetPlansItersMakeKML.write();
//
//			System.out.println(" ... done");


			// social network statistics
//			System.out.println(" Opening the files for the social network statistics...");
//			SocialNetworkStatistics snetstat=new SocialNetworkStatistics(Gbl.getConfig().socnetmodule().getOutDir());
//			snetstat.openFiles();

//			System.out.println(" ... done");
//			System.out.println(" Calculating and reporting network statistics ...");
//			snetstat.calculate(500, snet, plans);
//			System.out.println(" ... done");

//			PajekWriter pjw = new PajekWriter(Gbl.getConfig().socnetmodule().getOutDir(), (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE));

//			System.out.println(" Writing out social network for iteration " + 500 + " ...");
//			pjw.write(snet.getLinks(), plans, 500);
//			pjw.writeGeo(plans,snet, 500);
//			System.out.println(" ... done");
//			snetstat.closeFiles();

			//Extract the ego's social net for more analyses
//			Plans socialPlans = new PersonGetEgoNetGetPlans().extract(ego, plans);
//
//			plans.addAlgorithm(new PersonCalcActivitySpace("all"));
		PersonCalcASD2 pcalasd2=new PersonCalcASD2();
//			plans.addAlgorithm(pcalasd2);
//
//			System.out.println("  done.");


			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running matrices algos... ");
//			new MatricesCompleteBasedOnFacilities(facilities, (ZoneLayer)Gbl.getWorld().getLayer("municipality")).run(Matrices.getSingleton());
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////
//			System.out.println("  finishing person algorithms...");
			pcalasd2.run(plans);
			double xxx=pcalasd2.smASD2.average();
			System.out.println("##Result "+xxx);
//			pwast.close();
			System.out.println("  done.");

//			Scenario.writePlans(socialPlans);
//			Scenario.writeNetwork(network);
//			Scenario.writeFacilities(facilities);
//			Scenario.writeWorld(Gbl.getWorld());
//			Scenario.writeConfig();
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

