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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dgrether
 */
public class TravelTimeTest {

  @Test
	public void testEquilOneAgent() {
		Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("test/scenarios/equil/config.xml");
		ScenarioImpl data = (ScenarioImpl) sl.getScenario();
		Config conf = data.getConfig();

		String popFileName = "test/scenarios/equil/plans1.xml";
		conf.plans().setInputFile(popFileName);

		sl.loadScenario();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventTestHandler(agentTravelTimes));

	  QSimUtils.createDefaultQSim(data, events).run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Person.class));
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
		Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("test/scenarios/equil/config.xml");
		ScenarioImpl data = (ScenarioImpl) sl.getScenario();
		Config conf = data.getConfig();

		String popFileName = "test/scenarios/equil/plans2.xml";
		conf.plans().setInputFile(popFileName);

		sl.loadScenario();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventTestHandler(agentTravelTimes));

	  QSimUtils.createDefaultQSim(data, events).run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Person.class));
		Assert.assertEquals(360.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(15, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13561.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);


		travelTimes = agentTravelTimes.get(Id.create("2", Person.class));
		Assert.assertEquals(360.0, travelTimes.get(Id.create(5, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(14, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13561.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1260.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(360.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}


	private static class EventTestHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes;

		public EventTestHandler(Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.agentTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.agentTravelTimes.put(event.getPersonId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
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
