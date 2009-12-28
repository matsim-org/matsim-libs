package playground.jhackney.postprocessing;
/* *********************************************************************** *
 * project: org.matsim.*
 * CreateKMZFromOutput.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.Scenario;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;

public class CreateKMZFromOutput {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make friend face to face scores each 10 iters:");


		Scenario.setUpScenarioConfig();
		Config config = Scenario.getConfig();

//		config.plans().setInputFile(Scenario.getSNInDir() + "output_plans"+i+".xml");

		World world = Scenario.readWorld();
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();
		new WorldConnectLocations().run(world);

		
//		Write out the KML for the EgoNet of a chosen agent
		System.out.println("  Initializing the KML output");

		EgoNetPlansItersMakeKML.setUp(Scenario.getConfig(), network);
		EgoNetPlansItersMakeKML.generateStyles();

		System.out.println("... done");
		
		for(int i=0; i<501; i+=50){
			config.socnetmodule().setInitIter(Integer.toString(i));
//			config.socnetmodule().setInitIter(Integer.toString(0));
//			Knowledges kn = new KnowledgesImpl();
			PopulationImpl plans = Scenario.readPlansAndKnowledges();
			//read in social network
			System.out.println(" Initializing the social network ...");
			SocialNetwork snet=new SocialNetwork(plans);

			System.out.println("... done");

			//read in facilities knowledge
//			new InitializeKnowledge(plans);

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


//			Person ego=plans.getPerson("21924270");



//			System.out.println(" Writing out KMZ activity spaces and day plans for agent's egoNet");
			Person testP=plans.getPersons().get(new IdImpl("21924270"));//1pct
//			Person testP=this.controler.getPopulation().getPerson("21462061");//10pct
			EgoNetPlansItersMakeKML.loadData(testP,i, Scenario.getKnowledges());
			System.out.println(" ... done");


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
//			Plans egoPlans = new PersonGetEgoNetGetPlans().extract(ego, plans);

//			socialPlans.addAlgorithm(new PersonCalcActivitySpace("all"));

//			System.out.println("  done.");


			//////////////////////////////////////////////////////////////////////

//			System.out.println("  running matrices algos... ");
//			new MatricesCompleteBasedOnFacilities(facilities, (ZoneLayer)Gbl.getWorld().getLayer("municipality")).run(Matrices.getSingleton());
//			System.out.println("  done.");

			//////////////////////////////////////////////////////////////////////
//			System.out.println("  finishing person algorithms...");
//			socialPlans.runAlgorithms();
//			pwast.close();
//			System.out.println("  done.");

//			Scenario.writePlans(socialPlans);
//			Scenario.writeNetwork(network);
//			Scenario.writeFacilities(facilities);
//			Scenario.writeWorld(Gbl.getWorld());
//			Scenario.writeConfig();

		}
		EgoNetPlansItersMakeKML.write();
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



