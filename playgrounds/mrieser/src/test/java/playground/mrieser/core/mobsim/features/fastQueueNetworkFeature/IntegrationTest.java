/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.testcases.utils.EventsCollector;

import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.CarDepartureHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanMobsimImpl;
import playground.mrieser.core.mobsim.impl.PopulationAgentSource;
import playground.mrieser.core.mobsim.integration.Fixture;

public class IntegrationTest {

	@Test
	public void test_StuckEventOnLinkAtSimulationEnd_SingleCPU() {
		Fixture f = new Fixture();
		Person person1 = f.addPersonWithOneLeg();

		EventsManager events = new EventsManagerImpl();
		EventsCollector eventsCollector = new EventsCollector();
		events.addHandler(eventsCollector);

		/* setup start */
		PlanMobsimImpl planSim = new PlanMobsimImpl(f.scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, events);
		planSim.setMobsimEngine(engine);
		engine.setStopTime(7.0 * 3600 + 10);

		// setup network
		NetworkFeature netFeature = new FastQueueNetworkFeature(f.scenario.getNetwork(), engine);

		// setup features; order is important!
		planSim.addMobsimFeature(new StatusFeature());
		planSim.addMobsimFeature(netFeature);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addMobsimFeature(ah);

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, f.scenario));
		/* setup end */  // TODO too much boilerplate code for testing

		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(f.scenario.getPopulation(), 1.0));

		planSim.runMobsim();

		List<Event> allEvents = eventsCollector.getEvents();
		Assert.assertEquals(6, allEvents.size());
		Assert.assertTrue(allEvents.get(0) instanceof ActivityEndEvent);
		Assert.assertTrue(allEvents.get(1) instanceof AgentDepartureEvent);
		Assert.assertTrue(allEvents.get(2) instanceof AgentWait2LinkEvent);
		Assert.assertTrue(allEvents.get(3) instanceof LinkLeaveEvent);
		Assert.assertTrue(allEvents.get(4) instanceof LinkEnterEvent);
		Assert.assertTrue(allEvents.get(5) instanceof AgentStuckEvent);
		Assert.assertEquals(person1.getId(), ((AgentStuckEvent) allEvents.get(5)).getPersonId());
	}

	@Test
	public void test_StuckEventOnLinkAtSimulationEnd_Multithreaded() {
		Fixture f = new Fixture();
		Person person1 = f.addPersonWithOneLeg();

		EventsManager events = new EventsManagerImpl();
		EventsCollector eventsCollector = new EventsCollector();
		events.addHandler(eventsCollector);

		/* setup start */
		PlanMobsimImpl planSim = new PlanMobsimImpl(f.scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, events);
		planSim.setMobsimEngine(engine);
		engine.setStopTime(7.0 * 3600 + 10);

		// setup network
		NetworkFeature netFeature = new FastQueueNetworkFeature(f.scenario.getNetwork(), engine, 2);

		// setup features; order is important!
		planSim.addMobsimFeature(new StatusFeature());
		planSim.addMobsimFeature(netFeature);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addMobsimFeature(ah);

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, f.scenario));
		/* setup end */  // TODO too much boilerplate code for testing

		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(f.scenario.getPopulation(), 1.0));

		planSim.runMobsim();

		List<Event> allEvents = eventsCollector.getEvents();
		for (Event e : allEvents) {
			System.out.println(e);
		}
		Assert.assertEquals(6, allEvents.size());
		Assert.assertTrue(allEvents.get(0) instanceof ActivityEndEvent);
		Assert.assertTrue(allEvents.get(1) instanceof AgentDepartureEvent);
		Assert.assertTrue(allEvents.get(2) instanceof AgentWait2LinkEvent);
		Assert.assertTrue(allEvents.get(3) instanceof LinkLeaveEvent);
		Assert.assertTrue(allEvents.get(4) instanceof LinkEnterEvent);
		Assert.assertTrue(allEvents.get(5) instanceof AgentStuckEvent);
		Assert.assertEquals(person1.getId(), ((AgentStuckEvent) allEvents.get(5)).getPersonId());
	}
}
