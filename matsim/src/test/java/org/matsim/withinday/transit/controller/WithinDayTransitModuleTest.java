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

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.pt.withinday.WithinDayTransitConfigGroup;
import org.matsim.pt.withinday.WithinDayTransitModule;
import org.matsim.testcases.MatsimTestUtils;

public class WithinDayTransitModuleTest {

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
		Config config = utils.loadConfig("test/scenarios/pt-withinday/config.xml", new WithinDayTransitConfigGroup());
		config.controler().setLastIteration(0);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new WithinDayTransitModule());
		PersonEntersVehicleEventHandler handler = this::checkEnterEvent;
		controler.getEvents().addHandler(handler);
		controler.run();
	}
	
	private void checkEnterEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().equals(Id.createPersonId(1))) {
			// This person should take the yellow line, e.g. vehicle 1000 (or 2000)
			assertEquals("Person 1 should take the yellow line", event.getVehicleId(), Id.createVehicleId(1000));
		}
		else if (event.getPersonId().equals(Id.createPersonId(2))) {
			// This person should be rerouted to the red line e.g. vehicle 3000
			assertEquals("Person 2 should take the red line after within-day replanning", event.getVehicleId(), Id.createVehicleId(3000));
		}
		log.info("The test handler detected that person "+event.getPersonId()+" enters vehicle "+event.getVehicleId());
	}
	
}
