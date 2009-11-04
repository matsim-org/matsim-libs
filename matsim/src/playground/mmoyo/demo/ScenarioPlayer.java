/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioPlayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.demo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.marcel.OTFDemo;
import playground.marcel.pt.queuesim.TransitQueueSimulation;
import playground.marcel.pt.routes.ExperimentalTransitRouteFactory;
import playground.marcel.pt.utils.CreateVehiclesForSchedule;

public class ScenarioPlayer {

	private static final String SERVERNAME = "ScenarioPlayer";

	public static void play(final ScenarioImpl scenario, final EventsImpl events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
	}

	public static void main(final String[] args){
		String networkFile = null;
		String scheduleFile = null;
		String outputPlansFile = null; 
		String outputDirectory = null;
		
		if ((args != null) && (args.length == 4)) {
			networkFile = args[0];
			scheduleFile = args[1];
			outputPlansFile = args[2];
			outputDirectory = args[3];
		}else {
			//exception
		}

		//ScenarioLoader sl = new ScenarioLoader(configFile);
		//ScenarioImpl scenario = sl.getScenario();
		ScenarioImpl scenario = new ScenarioImpl();
		ScenarioLoader sl = new ScenarioLoader(scenario);
		
		NetworkLayer network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		
		//////////////////////////////////////////
		Config config = scenario.getConfig();

		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(outputPlansFile);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.simulation().setStartTime(0.0);
		config.simulation().setEndTime(43200);  //12:00
		config.simulation().setSnapshotPeriod(0.0);
		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotStyle("queue");
		
		config.setParam("planCalcScore", "learningRate" ,"1.0");
		config.setParam("planCalcScore", "BrainExpBeta" ,"2.0");
		config.setParam("planCalcScore", "lateArrival" ,"-18");
		config.setParam("planCalcScore", "earlyDeparture" ,"-0");
		config.setParam("planCalcScore", "performing" ,"+6");
		config.setParam("planCalcScore", "traveling" ,"-6");
		config.setParam("planCalcScore", "waiting" ,"-0");
		config.setParam("planCalcScore", "activityType_0"            ,"h");
		config.setParam("planCalcScore", "activityPriority_0"        ,"1");
		config.setParam("planCalcScore", "activityTypicalDuration_0" ,"12:00:00");
		config.setParam("planCalcScore", "activityMinimalDuration_0" ,"08:00:00" );
		config.setParam("planCalcScore", "activityPriority_1"        ,"1");
		config.setParam("planCalcScore", "activityTypicalDuration_1" ,"08:00:00");
		config.setParam("planCalcScore", "activityMinimalDuration_1" ,"06:00:00");
		config.setParam("planCalcScore", "activityOpeningTime_1"     ,"07:00:00");
		config.setParam("planCalcScore", "activityLatestStartTime_1" ,"09:00:00");
		config.setParam("planCalcScore", "activityEarliestEndTime_1" ,"");
		config.setParam("planCalcScore", "activityClosingTime_1"     ,"18:00:00");
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.setParam("strategy", "ModuleProbability_1" ,"0.9");
		config.setParam("strategy", "Module_1" ,"BestScore");
		config.setParam("strategy", "ModuleProbability_2" ,"0.1");
		config.setParam("strategy", "Module_2" ,"ReRoute");
	
		//////////////////////////////////////////
		sl.loadScenario();
		
		
		TransitSchedule schedule = scenario.getTransitSchedule();
	
		try {
			new TransitScheduleReaderV1(schedule, network).parse(scheduleFile);
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}

		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();
		final EventsImpl events = new EventsImpl();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);

		play(scenario, events);

		writer.closeFile();
	}

}
