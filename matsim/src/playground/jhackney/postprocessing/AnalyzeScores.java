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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.config.Config;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonCalcActivitySpace;
import org.matsim.population.algorithms.PersonDrawActivtiySpaces;
import org.matsim.population.algorithms.PersonWriteActivitySpaceTable;
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

		System.out.println("Make friend face to face scores each 10 iters:");


		Scenario.setUpScenarioConfig();
		Config config =Gbl.getConfig();

		Scenario.readWorld();
		Scenario.readFacilities();
		NetworkLayer network =Scenario.readNetwork();
		new WorldBottom2TopCompletion().run(Gbl.getWorld());

		for(int i=0; i<501; i+=10){
			config.socnetmodule().setInitIter(Integer.toString(i));
//			config.socnetmodule().setInitIter(Integer.toString(0));
			Population plans = Scenario.readPlans(i);
			//read in social network
			System.out.println(" Initializing the social network ...");
			SocialNetwork snet=new SocialNetwork(plans);

			System.out.println("... done");

			double totaliterationfriendscore=0;
			double totaliterationscore=0;
			int numplans=0;
			SpatialScorer scorer =new SpatialScorer();
			scorer.scoreActs(plans, i);
			//for each plan this is how it is calculated in the TRB runs, which is incorrect!!
			Iterator<Person> planiter=plans.iterator();
			while(planiter.hasNext()){
				//
				Person p = (Person) planiter.next();
				Plan plan = p.getSelectedPlan();
				ActIterator ait = plan.getIteratorAct();
				double friendscore=0;
				double nFriends=0;
				Hashtable<Act,ArrayList<Double>> actStats = scorer.calculateTimeWindowActStats(plan);
				while(ait.hasNext()){
					Act act = (Act)ait.next();
					if(act.getType().equals("leisure")){
//						double nFriends=scorer.calculateTimeWindowStats(plan).get(1);
						nFriends+=actStats.get(act).get(1);
					}
				}
				friendscore=100.*Math.log(nFriends+1);
				totaliterationfriendscore+=friendscore;
				if(!(plan.getScore()!=plan.getScore())){
					totaliterationscore+=plan.getScore();
				}
				numplans++;
			}
			double avgfriendscore=totaliterationfriendscore/((double) numplans);
			double avgscore=totaliterationscore/((double) numplans);
			System.out.println("##Result "+i+" "+avgfriendscore+" "+avgscore);
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

