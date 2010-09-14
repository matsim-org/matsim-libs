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
package playground.jhackney.postprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.world.World;

import playground.jhackney.ScenarioConfig;
import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;

public class AnalyzeScores {

//	private static EventsPostProcess epp=null;
//	private static MakeTimeWindowsFromEvents teo=null;
//	private static LinkedHashMap<Act,ArrayList<Double>> actStats=null;
//	private static LinkedHashMap<Facility,ArrayList<TimeWindow>> twm=null;
//	private static EventsToScore scoring =null;

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	static Population plans;
	static ActivityFacilitiesImpl facilities;
	static Knowledges knowledges;
	static Network network;

	public static void run() throws Exception {

		System.out.println("Make friend face to face scores each 10 iters:");

		SocNetConfigGroup snConfig = ScenarioConfig.setUpScenarioConfig();
		Config config = ScenarioConfig.readConfig();

		World world = ScenarioConfig.readWorld();
		facilities = ScenarioConfig.readFacilities();
		network =ScenarioConfig.readNetwork();
//		new WorldConnectLocations(config).run(world);
		int iplans=500;
		int isoc=500;
		knowledges = new KnowledgesImpl();
		plans = ScenarioConfig.readPlansAndKnowledges(network, knowledges);

		System.out.println(" Initializing the social network ...");

		System.out.println(" Initializing agent knowledge about geography ...");
		initializeKnowledge(snConfig);
		System.out.println("... done");

		// Override the config to take the last iteration
		snConfig.setReadMentalMap("true");
		snConfig.setSocNetGraphAlgo("read");
		snConfig.setInitIter(Integer.toString(isoc));
		snConfig.setInDirName(ScenarioConfig.getSNInDir());

		SocialNetwork snet=new SocialNetwork(plans, facilities, snConfig);
		EventsManagerImpl events = new EventsManagerImpl();
		EventsMapStartEndTimes epp;
		MakeTimeWindowsFromEvents teo=null;
		LinkedHashMap<Activity,ArrayList<Double>> actStats=null;
		LinkedHashMap<Id,ArrayList<TimeWindow>> twm=null;
		playground.jhackney.scoring.EventsToScoreAndReport scoring =null;
//		EventsToScore scoring=null;

		//Register scoring function and other events handlers

		System.out.println(" ... Instantiation of events overlap tracking done");
		epp=new EventsMapStartEndTimes();
		teo=new MakeTimeWindowsFromEvents(plans);
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm,facilities);
		playground.jhackney.scoring.EventSocScoringFactory factory = new playground.jhackney.scoring.EventSocScoringFactory("leisure",actStats, snConfig);
//		ScoringFunctionFactory cnfactory = new CharyparNagelScoringFunctionFactory(ScenarioConfig.getConfig().charyparNagelScoring());
//		EventSocScoringFactory factory = new EventSocScoringFactory("leisure", cnfactory,actStats);
		scoring = new playground.jhackney.scoring.EventsToScoreAndReport(plans, factory, config.charyparNagelScoring().getLearningRate());
		System.out.println("  Instantiating social network EventsToScore for scoring the plans");
//		scoring = new EventsToScore(plans, factory);

		// read in events
		System.out.println(" Initializing the events ...");
		events = ScenarioConfig.readEvents(iplans, epp, scoring);
		System.out.println("... done");

		System.out.println("  Handling events");
		//Fill timeWindowMap
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		actStats.clear();
		actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(twm,facilities));
//		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm);
//		scoring = new EventsToScore(plans, factory);
		scoring.finish(snConfig);

//		System.out.println("writing out output plans");
//		ScenarioConfig.writePlans(plans);
//
		System.out.println(" ... done");

//		for(int i=0; i<501; i+=10){
//			config.socnetmodule().setInitIter(Integer.toString(i));
//			Population plans = ScenarioConfig.readPlans(i);
//			//read in social network
//			System.out.println(" Initializing the social network ...");
//			SocialNetwork snet=new SocialNetwork(plans);
//
//			System.out.println("... done");
//
//			double totaliterationfriendscore=0;
//			double totaliterationscore=0;
//			int numplans=0;
//			TrackActsOverlap scorer =new TrackActsOverlap();
//			scorer.trackActs(plans, i);
//			//for each plan this is how it is calculated in the TRB runs, which is incorrect!!
//			Iterator<Person> planiter=plans.iterator();
//			while(planiter.hasNext()){
//				//
//				Person p = (Person) planiter.next();
//				Plan plan = p.getSelectedPlan();
//				ActIterator ait = plan.getIteratorAct();
//				double friendscore=0;
//				double nFriends=0;
//				LinkedHashMap<Act,ArrayList<Double>> actStats = scorer.calculateTimeWindowActStats(plan);
//				while(ait.hasNext()){
//					Act act = (Act)ait.next();
//					if(act.getType().equals("leisure")){
////						double nFriends=scorer.calculateTimeWindowStats(plan).get(1);
//						nFriends+=actStats.get(act).get(1);
//					}
//				}
//				friendscore=100.*Math.log(nFriends+1);
//				totaliterationfriendscore+=friendscore;
//				if(!(plan.getScore()!=plan.getScore())){
//					totaliterationscore+=plan.getScore();
//				}
//				numplans++;
//			}
//			double avgfriendscore=totaliterationfriendscore/((double) numplans);
//			double avgscore=totaliterationscore/((double) numplans);
//			System.out.println("##Result "+i+" "+avgfriendscore+" "+avgscore);
//		}

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
	protected static void initializeKnowledge(SocNetConfigGroup snConfig) {
		new InitializeKnowledge(plans, facilities, knowledges, network, snConfig);
	}
}

