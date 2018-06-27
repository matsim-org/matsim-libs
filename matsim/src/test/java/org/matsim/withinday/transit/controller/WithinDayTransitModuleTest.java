/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayTransitModuleTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.withinday.transit.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.pt.withinday.WithinDayTransitConfigGroup;
import org.matsim.pt.withinday.WithinDayTransitModule;
import org.matsim.testcases.MatsimTestUtils;

public class WithinDayTransitModuleTest {

	private Map<String,String> vehicleObservations = new HashMap<>();
	private Map<String,String> firstActivities = new HashMap<>();
	private Map<String,String> lastActivities = new HashMap<>();

	private static Logger log = Logger.getLogger(WithinDayTransitModuleTest.class);
	
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
	
    /**
     * This is a test scenario with two agents and a simple line network where two lines
     * are operated: the yellow and red line. 
     * Agent 1 departs around 5:00 am and Agent 2 around 5:15. For both agents the yellow
     * line is the best option, but at 5:15 a disruption triggers and the reroute method
     * modifies agents plans to take the red line. Thus, if the rerouting is performed
     * succesfully, we should see Agent 1 popping up in the yellow line, and Agent 2 in
     * the red line. Since every departure of the lines has a unique vehicle, we can
     * easily check in a PersonEntersVehicleHandler wheter the agents pop up in the
     * correct line. 
     */

	@Test
	public void runExampleWithinDayTransitControler() {
		// This is the scenario with regular behavior
		runTest(null, this::regularCheckEnterEvent);
		// This is the scenario with greedy behavior
		runTest("greedy", this::greedyCheckEnterEvent);
		// This is the scenario with flexible behavior that should be equivalent to regular behavior (in this scenario)
		runTest("flexible(00:00:00)", this::regularCheckEnterEvent);
		// This is the scenario with flexible behavior that should be equivalent to greedy behavior (in this scenario)
		runTest("flexible(02:00:00)", this::greedyCheckEnterEvent);
		
		
		// If the purple line is disrupted all day and the yellow line starting at 5:15,
		// the greedy behavior should be equal to the regular behavior
		runTest("greedy", "disruptions2.xml", this::regularCheckEnterEvent);
		
	}
	
	public Controler getControler(String behavior, String disruptionsFile, PersonEntersVehicleEventHandler handler) {
    	Config config = utils.loadConfig("test/scenarios/pt-withinday/config.xml", new WithinDayTransitConfigGroup());
		if (behavior != null) {
			WithinDayTransitConfigGroup cfg = ConfigUtils.addOrGetModule(config, WithinDayTransitConfigGroup.class);
			cfg.setBehavior(behavior);
		}
		if (disruptionsFile != null) {
			WithinDayTransitConfigGroup cfg = ConfigUtils.addOrGetModule(config, WithinDayTransitConfigGroup.class);
			cfg.setDisruptionsFile(disruptionsFile);
		}
		config.controler().setLastIteration(0);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new WithinDayTransitModule(config));
		controler.getEvents().addHandler(handler);
		
		ActivityStartEventHandler actStart = this::activityStartEvent;
		ActivityEndEventHandler actEnd = this::activityEndEvent;
		controler.getEvents().addHandler(actStart);
		controler.getEvents().addHandler(actEnd);
		
		return controler;
    }
	
	private void activityStartEvent(ActivityStartEvent event) {
		lastActivities.put(event.getPersonId().toString(), event.getActType());
	}
	
	private void activityEndEvent(ActivityEndEvent event) {
		String pid = event.getPersonId().toString();
		if (!firstActivities.containsKey(pid)) {
			firstActivities.put(pid, event.getActType());
		}
	}
	
	private void regularCheckEnterEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().equals(Id.createPersonId(1))) {
			// This person should take the yellow line, e.g. vehicle 1000 (or 2000)
			assertEquals("Person 1 should take the yellow line", Id.createVehicleId(1000), event.getVehicleId());
		}
		else if (event.getPersonId().equals(Id.createPersonId(2))) {
			// This person should be rerouted to the red line e.g. vehicle 3000
			assertEquals("Person 2 should take the red line after within-day replanning", Id.createVehicleId(3000), event.getVehicleId());
		}
		vehicleObservations.put(event.getPersonId().toString(), event.getVehicleId().toString());
		log.info("The test handler detected that person "+event.getPersonId()+" enters vehicle "+event.getVehicleId());
	}
	
	public void runTest(String behavior, PersonEntersVehicleEventHandler handler) {
		runTest(behavior, null, handler);
	}
	
	public void runTest(String behavior, String disruptionsFile, PersonEntersVehicleEventHandler handler) {
		Controler controler = getControler(behavior, disruptionsFile, handler);
		vehicleObservations.clear();
		firstActivities.clear();
		lastActivities.clear();
		controler.run();
		assertTrue("Person 1 did enter a vehicle", vehicleObservations.containsKey("1"));
		assertTrue("Person 2 did enter a vehicle", vehicleObservations.containsKey("2"));	
		for (Entry<String,String> entry : firstActivities.entrySet()) {
			String pid = entry.getKey();
			String type = entry.getValue();
			assertEquals("Person "+pid+" has equal start and end activity types", type, lastActivities.get(pid));
		}
		File output = new File(utils.getOutputDirectory());
		deleteTree(output);
	}
	
	private static void deleteTree(File f) {
		if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				deleteTree(child);
			}
		}
		f.delete();
	}
	
	private void greedyCheckEnterEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().equals(Id.createPersonId(1))) {
			// This person should take the purple line, e.g. vehicle 4000 (or 5000 or 6000)
			assertEquals("Person 1 should take the purple line", Id.createVehicleId(4000), event.getVehicleId());
		}
		else if (event.getPersonId().equals(Id.createPersonId(2))) {
			// This person should be rerouted to the purple line e.g. vehicle 5000 (or 4000 or 6000)
			assertEquals("Person 2 should take the purple line after within-day replanning", Id.createVehicleId(5000), event.getVehicleId());
		}
		vehicleObservations.put(event.getPersonId().toString(), event.getVehicleId().toString());
		log.info("The test handler detected that person "+event.getPersonId()+" enters vehicle "+event.getVehicleId());
	}

}
