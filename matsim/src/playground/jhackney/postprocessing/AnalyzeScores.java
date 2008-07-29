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

import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonCalcActivitySpace;
import org.matsim.plans.algorithms.PersonDrawActivtiySpaces;
import org.matsim.plans.algorithms.PersonWriteActivitySpaceTable;
import org.matsim.router.PlansCalcRoute;
import org.matsim.socialnetworks.io.PajekWriter;
import org.matsim.socialnetworks.scoring.SpatialScorer;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;
import org.matsim.utils.charts.XYScatterChart;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.Scenario;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.PersonGetEgoNetGetPlans;
import playground.jhackney.algorithms.PlansPlotScoreDistance;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.kml.EgoNetPlansMakeKML;

public class AnalyzeScores {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make activity spaces for egoNet:");


		Scenario.setUpScenarioConfig();
		Config config =Gbl.getConfig();

//		config.plans().setInputFile(Scenario.getSNInDir() + "output_plans"+i+".xml");

		Scenario.readWorld();
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();
		new WorldBottom2TopCompletion().run(Gbl.getWorld());

		for(int i=500; i<510; i+=10){
//			config.socnetmodule().setInitIter(Integer.toString(i));
			config.socnetmodule().setInitIter(Integer.toString(0));
			double totalscore=0;
			int n=0;
			Plans plans = Scenario.readPlans(i);
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

//			Write out the KML for the EgoNet of a chosen agent
//			System.out.println("  Initializing the KML output");

//			EgoNetPlansItersMakeKML.setUp(Scenario.getConfig(), network);
//			EgoNetPlansItersMakeKML.generateStyles();

//			System.out.println("... done");

//			System.out.println(" Writing out KMZ activity spaces and day plans for agent's egoNet");
//			Person testP=plans.getPerson("21924270");//1pct
//			Person testP=this.controler.getPopulation().getPerson("21462061");//10pct
//			EgoNetPlansItersMakeKML.loadData(testP,500);
//			EgoNetPlansItersMakeKML.write();

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

			SpatialScorer scorer =new SpatialScorer();
			scorer.scoreActs(plans, i);
			//for each plan
			Iterator planiter=plans.iterator();
			while(planiter.hasNext()){
				//
				Person p = (Person) planiter.next();
				Plan plan = p.getSelectedPlan();
				double nFriends=scorer.calculateTimeWindowStats(plan).get(1);
				double betaLogNFriends= 100.;
				totalscore+=betaLogNFriends*Math.log(nFriends+1);
				n++;
//				System.out.println(i+" "+nFriends+" "+Math.log(nFriends+1)+" "+betaLogNFriends*Math.log(nFriends+1));
				//print nFriends and log(nFriends)
				//Maybe calculate it wrong, here, like in SocialzingSociringFunction, to be consistent
				//write it out Iter, NFriends, logNFriends, betaLogNFriends * logNFRiends
			}
			double avgscore=totalscore/((double) n);
			System.out.println("##Result "+i+" "+avgscore);
		}
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

