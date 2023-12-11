
/* *********************************************************************** *
 * project: org.matsim.*
 * EquilPlans1Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

	public class EquilPlans1Test extends AbstractJDEQSimTest {

	 @Test
	 void test_EmptyCarRoute() {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config_plans1.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

		this.runJDEQSim(scenario);
		
		assertEquals(1, eventsByPerson.size());
		
		super.checkAscendingTimeStamps();
		super.checkEventsCorrespondToPlans(scenario.getPopulation());
		
		// custom checks:
		boolean wasInLoop = false;
		int index = 0;
		for (List<Event> list : super.eventsByPerson.values()) {
			wasInLoop = true;
			// checking the time of the first event
			assertEquals(21600, list.get(index).getTime(), 0.9);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof VehicleEntersTrafficEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index++) instanceof ActivityStartEvent);
			assertTrue(list.get(index++) instanceof ActivityEndEvent);
			assertTrue(list.get(index++) instanceof PersonDepartureEvent);
			assertTrue(list.get(index++) instanceof VehicleEntersTrafficEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof LinkLeaveEvent);
			assertTrue(list.get(index++) instanceof LinkEnterEvent);
			assertTrue(list.get(index++) instanceof PersonArrivalEvent);
			assertTrue(list.get(index) instanceof ActivityStartEvent);
			// checking the time of the last event
			assertEquals(38039, list.get(index).getTime(), 0.9);
		}

		assertTrue(wasInLoop);
	}

}
