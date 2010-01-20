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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.Scenario;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.TimeWindowCalcTimeCorrelations;
import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;

public class AnalyzeTimeCorrelations {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	static PopulationImpl plans;
	static ActivityFacilitiesImpl facilities;
	static Knowledges knowledges;
	static Network network;
	
	public static void run() throws Exception {

		System.out.println("Make friend face to face scores each 10 iters:");

		Scenario.setUpScenarioConfig();
		Config config = Scenario.getConfig();

		World world = Scenario.readWorld();
		facilities= Scenario.readFacilities();
		network =Scenario.readNetwork();
		new WorldConnectLocations().run(world);

		int iplan=500;
		int isoc=0;
//		read in plans
		System.out.println(" Initializing the plans ...");
		plans = Scenario.readPlansAndKnowledges();
		System.out.println("... done");

		System.out.println(" Initializing agent knowledge about geography ...");
		initializeKnowledge();
		System.out.println("... done");
		
		// read in events
		EventsManagerImpl events = new EventsManagerImpl();
//		TrackEventsOverlap teo = new TrackEventsOverlap();
		EventsMapStartEndTimes epp=new EventsMapStartEndTimes(plans);
		//Fill timeWindowMap

		System.out.println(" Initializing the events ...");
		events = Scenario.readEvents(iplan, epp);
		System.out.println("... done");

		System.out.println("  Handling events");
		
		System.out.println(" ... done");

		//read in social network
		config.socnetmodule().setInitIter(Integer.toString(isoc));
		System.out.println(" Initializing the social network ...");
		SocialNetwork snet=new SocialNetwork(plans, facilities);
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
		new TimeWindowCalcTimeCorrelations(teo.getTimeWindowMap(), out2, out1, facilities);
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
	protected static void initializeKnowledge() {
		new InitializeKnowledge(plans, facilities, knowledges, network);
	}
}

