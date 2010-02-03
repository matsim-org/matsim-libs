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

package org.matsim.core.mobsim.queuesim;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 */
public class TravelTimeTest {

	@Test
	public void testEquilOneAgent() {
		Map<Id, Map<Id, Double>> agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl("test/scenarios/equil/config.xml");
		ScenarioImpl data = sl.getScenario();
		Config conf = data.getConfig();

		String popFileName = "test/scenarios/equil/plans1.xml";
		conf.plans().setInputFile(popFileName);

		sl.loadScenario();

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(new EventTestHandler(agentTravelTimes));

		new QueueSimulation(data, events).run();

		Map<Id, Double> travelTimes = agentTravelTimes.get(new IdImpl("1"));
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(6)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(new IdImpl(15)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testEquilTwoAgents() {
		Map<Id, Map<Id, Double>> agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl("test/scenarios/equil/config.xml");
		ScenarioImpl data = sl.getScenario();
		Config conf = data.getConfig();

		String popFileName = "test/scenarios/equil/plans2.xml";
		conf.plans().setInputFile(popFileName);

		sl.loadScenario();

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(new EventTestHandler(agentTravelTimes));

		new QueueSimulation(data, events).run();

		Map<Id, Double> travelTimes = agentTravelTimes.get(new IdImpl("1"));
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(6)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(new IdImpl(15)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), MatsimTestUtils.EPSILON);


		travelTimes = agentTravelTimes.get(new IdImpl("2"));
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(5)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(new IdImpl(14)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), MatsimTestUtils.EPSILON);
	}

	private static class EventTestHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id, Map<Id, Double>> agentTravelTimes;

		public EventTestHandler(Map<Id, Map<Id, Double>> agentTravelTimes) {
			this.agentTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
			if (travelTimes == null) {
				travelTimes = new HashMap<Id, Double>();
				this.agentTravelTimes.put(event.getPersonId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}

}
