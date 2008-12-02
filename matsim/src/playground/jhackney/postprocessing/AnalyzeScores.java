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
import org.matsim.events.Events;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
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
import org.matsim.scoring.EventsToScore;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.algorithms.EventsPostProcess;
import org.matsim.socialnetworks.io.PajekWriter;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import org.matsim.socialnetworks.scoring.TrackActsOverlap;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;
import org.matsim.utils.charts.XYScatterChart;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.ScenarioConfig;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.PersonGetEgoNetGetPlans;
import playground.jhackney.algorithms.PlansPlotScoreDistance;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.kml.EgoNetPlansMakeKML;
import playground.jhackney.scoring.CharyparNagelScoringFunctionFactory;

public class AnalyzeScores {

//	private static EventsPostProcess epp=null;
//	private static MakeTimeWindowsFromEvents teo=null;
//	private static Hashtable<Act,ArrayList<Double>> actStats=null;
//	private static Hashtable<Facility,ArrayList<TimeWindow>> twm=null;
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
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
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
		EventsPostProcess epp;
		MakeTimeWindowsFromEvents teo=null;
		Hashtable<Act,ArrayList<Double>> actStats=null;
		Hashtable<Facility,ArrayList<TimeWindow>> twm=null;
		playground.jhackney.scoring.EventsToScoreAndReport scoring =null;
		
		//Register scoring function and other events handlers
//		playground.jhackney.scoring.CharyparNagelScoringFunctionFactory scoringFf=new playground.jhackney.scoring.CharyparNagelScoringFunctionFactory();
		
		//TODO superfluous in 0th iteration and not necessary anymore except that scoring function needs it (can null be passed?)
//		teo=new MakeTimeWindowsFromEvents(epp);
//		twm=teo.getTimeWindowMap();

		System.out.println(" ... Instantiation of events overlap tracking done");
		epp=new EventsPostProcess(plans);
		teo=new MakeTimeWindowsFromEvents();
		teo.calculate(epp);
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
		teo.calculate(epp);
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
//				Hashtable<Act,ArrayList<Double>> actStats = scorer.calculateTimeWindowActStats(plan);
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

