/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether
 */
public class TravelTimeTest {

	@Test
	public void testEquilOneAgent() {
		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();

		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans1.xml";
		config.plans().setInputFile(popFileName);

		ScenarioUtils.loadScenario(scenario);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventTestHandler(agentTravelTimes));

		QSimUtils.createDefaultQSim(scenario, events).run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(360.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(15, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13561.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testEquilTwoAgents() {
		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();

		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans2.xml";
		config.plans().setInputFile(popFileName);

		ScenarioUtils.loadScenario(scenario);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventTestHandler(agentTravelTimes));

		QSimUtils.createDefaultQSim(scenario, events).run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(360.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(15, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13561.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);


		travelTimes = agentTravelTimes.get(Id.create("2", Vehicle.class));
		Assert.assertEquals(360.0, travelTimes.get(Id.create(5, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(14, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13561.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}


	private static class EventTestHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes;

		public EventTestHandler(Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes) {
			this.vehicleTravelTimes = vehicleTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get(event.getVehicleId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.vehicleTravelTimes.put(event.getVehicleId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), event.getTime());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get(event.getVehicleId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d;
					travelTimes.put(event.getLinkId(), time);
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}

}
