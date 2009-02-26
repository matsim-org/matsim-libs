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
import java.util.LinkedHashMap;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.algorithms.EventsMapStartEndTimes;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.ScenarioConfig;

public class AnalyzeScores {

//	private static EventsPostProcess epp=null;
//	private static MakeTimeWindowsFromEvents teo=null;
//	private static LinkedHashMap<Act,ArrayList<Double>> actStats=null;
//	private static LinkedHashMap<Facility,ArrayList<TimeWindow>> twm=null;
//	private static EventsToScore scoring =null;

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

		System.out.println("Make friend face to face scores each 10 iters:");


		ScenarioConfig.setUpScenarioConfig();
		ScenarioConfig.readConfig();
		Config config =Gbl.getConfig();

		ScenarioConfig.readWorld();
		ScenarioConfig.readFacilities();
		NetworkLayer network =ScenarioConfig.readNetwork();
		new WorldConnectLocations().run(Gbl.getWorld());
		int i=500;
		Population plans = ScenarioConfig.readPlans(i);
		System.out.println(" Initializing the social network ...");
		
		// Override the config to take the last iteration
		config.socnetmodule().setReadMentalMap("true");
		config.socnetmodule().setSocNetGraphAlgo("read");
		config.socnetmodule().setInitIter(Integer.toString(i));
		config.socnetmodule().setInDirName(ScenarioConfig.getSNInDir());
		
		SocialNetwork snet=new SocialNetwork(plans);
		Events events = new Events();
		EventsMapStartEndTimes epp;
		MakeTimeWindowsFromEvents teo=null;
		LinkedHashMap<Act,ArrayList<Double>> actStats=null;
		LinkedHashMap<Facility,ArrayList<TimeWindow>> twm=null;
		playground.jhackney.scoring.EventsToScoreAndReport scoring =null;
		
		//Register scoring function and other events handlers
//		playground.jhackney.scoring.CharyparNagelScoringFunctionFactory scoringFf=new playground.jhackney.scoring.CharyparNagelScoringFunctionFactory();
		
		//TODO superfluous in 0th iteration and not necessary anymore except that scoring function needs it (can null be passed?)
//		teo=new MakeTimeWindowsFromEvents(epp);
//		twm=teo.getTimeWindowMap();

		System.out.println(" ... Instantiation of events overlap tracking done");
		epp=new EventsMapStartEndTimes(plans);
		teo=new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm);
		playground.jhackney.scoring.EventSocScoringFactory factory = new playground.jhackney.scoring.EventSocScoringFactory("leisure",actStats);
		scoring = new playground.jhackney.scoring.EventsToScoreAndReport(plans, factory);
		System.out.println("  Instantiating social network EventsToScore for scoring the plans");
//		scoring = new EventsToScore(plans, factory);

		// read in events
//		TrackEventsOverlap teo = new TrackEventsOverlap();
//		epp=new EventsPostProcess(plans);

		System.out.println(" Initializing the events ...");
		events = ScenarioConfig.readEvents(i, epp, scoring);
		System.out.println("... done");

		System.out.println("  Handling events");
		//Fill timeWindowMap
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		actStats.clear();
		actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(twm));
//		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm);
//		scoring = new EventsToScore(plans, factory);
		scoring.finish();
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

}

