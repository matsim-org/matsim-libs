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

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.socialnetworks.algorithms.EventsMapStartEndTimes;
import org.matsim.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.Scenario;
import playground.jhackney.algorithms.TimeWindowCalcTimeCorrelations;

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
		new WorldConnectLocations().run(Gbl.getWorld());

		int i=500;
//		read in plans
		System.out.println(" Initializing the plans ...");
		Population plans = Scenario.readPlans(i);
		System.out.println("... done");

		// read in events
		Events events = new Events();
//		TrackEventsOverlap teo = new TrackEventsOverlap();
		EventsMapStartEndTimes epp=new EventsMapStartEndTimes(plans);
		//Fill timeWindowMap

		System.out.println(" Initializing the events ...");
		events = Scenario.readEvents(i, epp);
		System.out.println("... done");

		System.out.println("  Handling events");
		
		System.out.println(" ... done");

		//read in social network
		config.socnetmodule().setInitIter(Integer.toString(i));
		System.out.println(" Initializing the social network ...");
		SocialNetwork snet=new SocialNetwork(plans);
		System.out.println("... done");

//		double totaliterationfriendscore=0;
//		double totaliterationscore=0;
//		int numplans=0;

//		LinkedHashMap<Act,ArrayList<Double>> actStats = CompareTimeWindows.calculateTimeWindowEventActStats(teo.getTimeWindowMap());
// loop through timeWindowMap
		// for each Activity 
//		new MakeTimeWindowsFromEvents(epp);
		String out2=Scenario.getOut2();
		String out1=Scenario.getOut1();
		MakeTimeWindowsFromEvents teo= new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		new TimeWindowCalcTimeCorrelations(teo.getTimeWindowMap(), out2, out1);
		System.out.println("Type\tId");
		new WriteActivityLocationsByType(plans);
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

