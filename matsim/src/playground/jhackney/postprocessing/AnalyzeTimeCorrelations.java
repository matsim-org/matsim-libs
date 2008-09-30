package playground.jhackney.postprocessing;

/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeTimeCorrelations.java
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
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.io.PajekWriter;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.scoring.TrackActsOverlap;
import org.matsim.socialnetworks.scoring.TrackEventsOverlap;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;
import org.matsim.utils.charts.XYScatterChart;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.Scenario;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.PersonGetEgoNetGetPlans;
import playground.jhackney.algorithms.PlansPlotScoreDistance;
import playground.jhackney.algorithms.TimeWindowCalcTimeCorrelations;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.kml.EgoNetPlansMakeKML;

public class AnalyzeTimeCorrelations {

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

		int i=500;
//		read in plans
		System.out.println(" Initializing the plans ...");
		Population plans = Scenario.readPlans(i);
		System.out.println("... done");

		// read in events
		Events events = new Events();
		TrackEventsOverlap teo = new TrackEventsOverlap();
		//Fill timeWindowMap
		events.addHandler(teo);

		System.out.println(" Initializing the events ...");
		events = Scenario.readEvents(i);
		System.out.println("... done");

		System.out.println(" ... Instantiation of events overlap tracking done");

		//read in social network
		config.socnetmodule().setInitIter(Integer.toString(i));
		System.out.println(" Initializing the social network ...");
		SocialNetwork snet=new SocialNetwork(plans);
		System.out.println("... done");

//		double totaliterationfriendscore=0;
//		double totaliterationscore=0;
//		int numplans=0;

//		Hashtable<Act,ArrayList<Double>> actStats = CompareTimeWindows.calculateTimeWindowEventActStats(teo.getTimeWindowMap());
// loop through timeWindowMap
		// for each Activity 

		new TimeWindowCalcTimeCorrelations(teo.getTimeWindowMap());
		
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

