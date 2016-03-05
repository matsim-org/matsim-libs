/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.LogCounter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

@RunWith(Parameterized.class)
public class QSimTest {

	private final static Logger log = Logger.getLogger(QSimTest.class);

	private final boolean isUsingFastCapacityUpdate;
	
	public QSimTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}
	
	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}
	
	private QSim createQSim(MutableScenario scenario, EventsManager events) {
		QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	private QSim createQSim(Fixture f, EventsManager events) {
		Scenario sc = f.scenario;
		QSim qSim1 = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, events);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	@Test
	public void testSingleAgent() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 2, collector.events.size());
		Assert.assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in second event.", 6.0*3600 + 12, collector.events.get(1).getTime(), MatsimTestCase.EPSILON);
	}

	/**
	 * This test is mostly useful for manual debugging, because only two single agents are simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	@Test
	public void testTwoAgent() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add two persons with leg from link1 to link3, the first starting at 6am, the second at 7am
		for (int i = 0; i < 2; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
			a1.setEndTime((6+i)*3600);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 4, collector.events.size());
		Assert.assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in second event.", 6.0*3600 + 12, collector.events.get(1).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in first event.", 7.0*3600 + 1, collector.events.get(2).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in second event.", 7.0*3600 + 12, collector.events.get(3).getTime(), MatsimTestCase.EPSILON);
	}

	/**
	 * A single agent is simulated that uses teleportation for its one and only leg.
	 *
	 * @author mrieser
	 */
	@Test
	public void testTeleportationSingleAgent() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createAndAddLeg("other");
		Route route = f.scenario.getPopulation().getFactory().createRoute(Route.class, f.link1.getId(), f.link3.getId()); // TODO [MR] use different factory/mode here
        route.setTravelTime(15.0);
        leg.setRoute(route);
		plan.createAndAddActivity("w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		List<Event> allEvents = collector.getEvents();
		Assert.assertEquals("wrong number of events.", 5, collector.getEvents().size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
        Assert.assertEquals("wrong type of event.", TeleportationArrivalEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(4).getClass());
		Assert.assertEquals("wrong time in event.", 6.0*3600 + 0, allEvents.get(0).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in event.", 6.0*3600 + 0, allEvents.get(1).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in event.", 6.0*3600 + 15, allEvents.get(2).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in event.", 6.0*3600 + 15, allEvents.get(3).getTime(), MatsimTestCase.EPSILON);
	}

	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 * The agent ends its activity immediately, which is handled differently than agents staying
	 * for some time at their first activity location.
	 *
	 * @author cdobler
	 */
	@Test
	public void testSingleAgentImmediateDeparture() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(0);
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		f.config.qsim().setEndTime(10*3600);
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 2, collector.events.size());
		Assert.assertEquals("wrong time in first event.", 0.0*3600 + 1, collector.events.get(0).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in second event.", 0.0*3600 + 12, collector.events.get(1).getTime(), MatsimTestCase.EPSILON);
	}
	
	/**
	 * Simulates a single agent that has two activities on the same link.
	 * Tests if the simulation correctly handles such cases.
	 *
	 * <br>
	 * The primary reason for the existence of this test was that such trips were
	 * handled especially (not being actually simulated).
	 * Now, it is the opposite: it checks that such trips are really simulated
	 * properly. td apr'14
	 *
	 * @author mrieser
	 */
	@Test
	public void testSingleAgent_EmptyRoute() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a single person with leg from link1 to link1
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link1.getId());
		route.setLinkIds(f.link1.getId(), new ArrayList<Id<Link>>(0), f.link1.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}
		Assert.assertEquals("wrong number of events.", 8, allEvents.size());


		Assert.assertEquals("wrong type of 1st event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of 2nd event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
		Assert.assertEquals("wrong type of 3rd event.", PersonEntersVehicleEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of 4th event.", VehicleEntersTrafficEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of 5th event.", VehicleLeavesTrafficEvent.class, allEvents.get(4).getClass());
		Assert.assertEquals("wrong type of 6th event.", PersonLeavesVehicleEvent.class, allEvents.get(5).getClass());
		Assert.assertEquals("wrong type of 7th event.", PersonArrivalEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of 8th event.", ActivityStartEvent.class, allEvents.get(7).getClass());


		Assert.assertEquals("wrong time in 1st event.", 6.0*3600 + 0, allEvents.get(0).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 2nd event.", 6.0*3600 + 0, allEvents.get(1).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 3rd event.", 6.0*3600 + 0, allEvents.get(2).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 4th event.", 6.0*3600 + 0, allEvents.get(3).getTime(), MatsimTestCase.EPSILON);

		Assert.assertEquals("wrong time in 5th event.", 6.0*3600 + 1, allEvents.get(4).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 6th event.", 6.0*3600 + 1, allEvents.get(5).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 7th event.", 6.0*3600 + 1, allEvents.get(6).getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals("wrong time in 8th event.", 6.0*3600 + 1, allEvents.get(7).getTime(), MatsimTestCase.EPSILON);


		Assert.assertEquals("wrong link in 1st event.", f.link1.getId(), ((ActivityEndEvent) allEvents.get(0)).getLinkId() );
		Assert.assertEquals("wrong link in 2nd event.", f.link1.getId(), ((PersonDepartureEvent) allEvents.get(1)).getLinkId() );
		Assert.assertEquals("wrong link in 4th event.", f.link1.getId(), ((VehicleEntersTrafficEvent) allEvents.get(3)).getLinkId() );
		Assert.assertEquals("wrong link in 5th event.", f.link1.getId(), ((VehicleLeavesTrafficEvent) allEvents.get(4)).getLinkId() );
		Assert.assertEquals("wrong link in 7th event.", f.link1.getId(), ((PersonArrivalEvent) allEvents.get(6)).getLinkId() );
		Assert.assertEquals("wrong link in 8th event.", f.link1.getId(), ((ActivityStartEvent) allEvents.get(7)).getLinkId() );
	}

	/**
	 * Simulates a single agent whose route ends on a link that is actually a loop link.
	 * Yes, this can happen in real scenarios.
	 *
	 * @author mrieser
	 */
	@Test
	public void testSingleAgent_LastLinkIsLoop() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Link loopLink = f.network.createAndAddLink(Id.create("loop", Link.class), f.node4, f.node4, 100.0, 10.0, 500, 1);

		// add a single person with leg from link1 to loop-link
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), loopLink.getId());
		ArrayList<Id<Link>> links = new ArrayList<Id<Link>>();
		links.add(f.link2.getId());
		links.add(f.link3.getId());
		route.setLinkIds(f.link1.getId(), links, loopLink.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", loopLink.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		f.config.qsim().setEndTime(7*3600);
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}
		Assert.assertEquals("wrong number of events.", 14, allEvents.size());
		Assert.assertEquals("wrong type of 1st event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of 2nd event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(4).getClass()); // link 1
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(5).getClass()); // link 2
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(7).getClass()); // link 3
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(8).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(9).getClass()); // loop link
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(10).getClass());
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(11).getClass());
		Assert.assertEquals("wrong type of 11th event.", PersonArrivalEvent.class, allEvents.get(12).getClass());
		Assert.assertEquals("wrong type of 12th event.", ActivityStartEvent.class, allEvents.get(13).getClass());
	}

	/*package*/ static class LinkEnterEventCollector implements LinkEnterEventHandler {
		public final ArrayList<LinkEnterEvent> events = new ArrayList<LinkEnterEvent>();
		@Override
		public void handleEvent(final LinkEnterEvent event) {
			this.events.add(event);
		}
		@Override
		public void reset(final int iteration) {
			this.events.clear();
		}
	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all.
	 *
	 * @author mrieser
	 */
	@Test
	public void testAgentWithoutLeg() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		plan.createAndAddActivity("home", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all, but the only activity has an end time set.
	 *
	 * @author mrieser
	 */
	@Test
	public void testAgentWithoutLegWithEndtime() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl act = plan.createAndAddActivity("home", f.link1.getId());
		act.setEndTime(6.0 * 3600);
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that no Exception occurs if the last activity of an agent has an end time set (which is wrong).
	 *
	 * @author mrieser
	 */
	@Test
	public void testAgentWithLastActWithEndtime() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl act = plan.createAndAddActivity("home", f.link1.getId());
		act.setEndTime(6.0 * 3600);
		LegImpl leg = plan.createAndAddLeg(TransportMode.walk);
		leg.setRoute(new GenericRouteImpl(f.link1.getId(), f.link2.getId()));
		leg.getRoute().setTravelTime(0.); // retrofitting to repair failing test.  kai, apr'15
		act = plan.createAndAddActivity("work", f.link2.getId());
		act.setEndTime(6.0 * 3600 + 60);
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		Assert.assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	@Test
	public void testFlowCapacityDriving() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 10000; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			ActivityImpl a = plan.createAndAddActivity("h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		if(this.isUsingFastCapacityUpdate) {
			Assert.assertEquals(3001, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(999, volume[8]); // all the rest
		} else {
			Assert.assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(1000, volume[8]); // all the rest
		}
	}

	
	/**
	 * Tests that on a link with a flow capacity of 0.25 vehicles per time step, after the first vehicle
	 * at time step t, the second vehicle may pass in time step t + 4 and the third in time step t+8.
	 *
	 * @author michaz
	 */
	@Test
	public void testFlowCapacityDrivingFraction() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		f.link2.setCapacity(900.0); // One vehicle every 4 seconds
		
		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 3; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			ActivityImpl a = plan.createAndAddActivity("h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(1, 7*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());

		Assert.assertEquals(1, volume[7*3600 - 1800]); // First vehicle
		Assert.assertEquals(1, volume[7*3600 - 1800 + 4]); // Second vehicle
		Assert.assertEquals(1, volume[7*3600 - 1800 + 8]); // Third vehicle
	}
	
	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents starting on a link. Due to the different handling of these
	 * agents and their direct placing in the Buffer, it makes sense to
	 * test this specifically.
	 *
	 * @author mrieser
	 */
	@Test
	public void testFlowCapacityStarting() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 10000; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			ActivityImpl a2 = plan.createAndAddActivity("h", f.link2.getId());
			a2.setEndTime(7*3600 - 1801);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
			route.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		if(this.isUsingFastCapacityUpdate) {
			Assert.assertEquals(3001, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(999, volume[8]); // all the rest
		} else {
			Assert.assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(1000, volume[8]); // all the rest
		}
	}

	/**
	 * Tests that the flow capacity of a link can be reached (but not exceeded) by
	 * agents starting on that link or driving through that link. This especially
	 * insures that the flow capacity measures both kinds (starting, driving) together.
	 *
	 * @author mrieser
	 */
	@Test
	public void testFlowCapacityMixed() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 5000; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			ActivityImpl a2 = plan.createAndAddActivity("h", f.link2.getId());
			a2.setEndTime(7*3600 - 1801);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
			route.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}
		// add a lot of persons with legs from link1 to link3
		for (int i = 5001; i <= 10000; i++) {
			Person person = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
			ActivityImpl a2 = plan.createAndAddActivity("h", f.link1.getId());
			a2.setEndTime(7*3600 - 1812);
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		if(this.isUsingFastCapacityUpdate) {
			Assert.assertEquals(3001, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(999, volume[8]); // all the rest
		} else {
			Assert.assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
			Assert.assertEquals(6000, volume[7]); // we should have maximum flow in this hour
			Assert.assertEquals(1000, volume[8]); // all the rest	
		}
		
	}

	/**
	 * Tests that vehicles are teleported if needed so that agents can use the car wherever they want.
	 *
	 * @author mrieser
	 */
	@Test
	public void testVehicleTeleportationTrue() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg("other");
		l1.setTravelTime(10);
		l1.setRoute(((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link2.getId()));
		ActivityImpl a2 = plan.createAndAddActivity("w", f.link2.getId());
		a2.setEndTime(7.0*3600 + 20);
		LegImpl l2 = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route2 = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
		route2.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
		l2.setRoute(route2);
		plan.createAndAddActivity("l", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		List<Event> allEvents = collector.getEvents();
		Assert.assertEquals("wrong number of events.", 15, allEvents.size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
        Assert.assertEquals("wrong type of event.", TeleportationArrivalEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(4).getClass());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(5).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(7).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(8).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(9).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(10).getClass());
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(11).getClass());
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(12).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(13).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(14).getClass());
	}

	
	/**
	 * Tests that an agent whose car isn't available waits for it to become available.
	 *
	 * @author michaz
	 */
	@Test
	public void testWaitingForCar() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		f.scenario.getConfig().qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.wait);
		f.scenario.getConfig().qsim().setEndTime(24.0 * 60.0 * 60.0);
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg("other");
		l1.setTravelTime(10);
		l1.setRoute(((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link2.getId()));
		ActivityImpl a2 = plan.createAndAddActivity("w", f.link2.getId());
		a2.setEndTime(7.0*3600 + 20);
		LegImpl l2 = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route2 = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
		route2.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
		l2.setRoute(route2);
		plan.createAndAddActivity("l", f.link3.getId());
		f.plans.addPerson(person);

		Person personWhoBringsTheCar = PopulationUtils.createPerson(Id.create(2, Person.class));
		PlanImpl planWhichBringsTheCar = PersonUtils.createAndAddPlan(personWhoBringsTheCar, true);
		ActivityImpl aa1 = planWhichBringsTheCar.createAndAddActivity("h", f.link1.getId());
		aa1.setEndTime(7.0*3600 + 30);
		LegImpl ll1 = planWhichBringsTheCar.createAndAddLeg(TransportMode.car);
		NetworkRoute route3 = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link2.getId());
		route3.setLinkIds(f.link1.getId(), f.linkIdsNone, f.link2.getId());
		route3.setVehicleId(Id.create(1, Vehicle.class)); // We drive the car that person 1 needs.
		ll1.setRoute(route3);
		ActivityImpl aa2 = planWhichBringsTheCar.createAndAddActivity("w", f.link2.getId());
		aa2.setEndTime(7.0*3600 + 60);
		LegImpl ll2 = planWhichBringsTheCar.createAndAddLeg("other");
		ll2.setTravelTime(10);
		ll2.setRoute(((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId()));
		planWhichBringsTheCar.createAndAddActivity("l", f.link3.getId());
		f.plans.addPerson(personWhoBringsTheCar);
		
		
		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}
		Assert.assertEquals("wrong number of events.", 30, allEvents.size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
        Assert.assertEquals("wrong type of event.", TeleportationArrivalEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(4).getClass());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(5).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(7).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(8).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(9).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(10).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(11).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(12).getClass());
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(13).getClass());
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(14).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(15).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(16).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(17).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(18).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(19).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(20).getClass());
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(21).getClass());
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(22).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(23).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(24).getClass());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(25).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(26).getClass());
        Assert.assertEquals("wrong type of event.", TeleportationArrivalEvent.class, allEvents.get(27).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(28).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(29).getClass());
	}
	
	/**
	 * Tests that vehicles are not teleported if they are missing, but that an Exception is thrown instead.
	 *
	 * @author mrieser
	 */
	@Test
	public void testVehicleTeleportationFalse() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		f.scenario.getConfig().qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.exception);
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg("other");
		l1.setTravelTime(10);
		l1.setRoute(((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link2.getId())); // TODO [MR] use different factory / TransportationMode
		ActivityImpl a2 = plan.createAndAddActivity("w", f.link2.getId());
		a2.setEndTime(7.0*3600 + 20);
		LegImpl l2 = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route2 = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
		route2.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
		l2.setRoute(route2);
		plan.createAndAddActivity("l", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		try {
			sim.run();
			Assert.fail("expected RuntimeException, but there was none.");
		} catch (RuntimeException e) {
			log.info("catched expected RuntimeException: " + e.getMessage());
		}

		List<Event> allEvents = collector.getEvents();
		Assert.assertEquals("wrong number of events.", 7, allEvents.size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
        Assert.assertEquals("wrong type of event.", TeleportationArrivalEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(4).getClass());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(5).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(6).getClass());
	}

	/**
	 * Tests that if a specific vehicle is assigned to an agent in its NetworkRoute, that this vehicle
	 * is used instead of a default one.
	 *
	 * @author mrieser
	 */
	@Test
	public void testAssignedVehicles() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class)); // do not add person to population, we'll do it ourselves for the test
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link2.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route1 = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
		route1.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
		route1.setVehicleId(Id.create(2, Vehicle.class));
		l1.setRoute(route1);
		plan.createAndAddActivity("w", f.link3.getId());

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();

		/* prepare sim */
		QSim sim = createQSim(f, events);
		NetsimNetwork qnet = sim.getNetsimNetwork();
		sim.prepareSim();
		NetsimLink qlink2 = qnet.getNetsimLink(Id.create(2, Link.class));
		NetsimLink qlink3 = qnet.getNetsimLink(Id.create(3, Link.class));

		VehicleType defaultVehicleType = new VehicleTypeImpl(Id.create("defaultVehicleType", VehicleType.class));
		QVehicle vehicle1 = new QVehicle(new VehicleImpl(Id.create(1, Vehicle.class), defaultVehicleType));
		QVehicle vehicle2 = new QVehicle(new VehicleImpl(Id.create(2, Vehicle.class), defaultVehicleType));
		sim.addParkedVehicle(vehicle1, Id.create(2, Link.class));
		sim.addParkedVehicle(vehicle2, Id.create(2, Link.class));

		sim.getSimTimer().setTime(100.0);
		PersonDriverAgentImpl agent = new PersonDriverAgentImpl(person.getSelectedPlan(), sim);
		sim.insertAgentIntoMobsim(agent); 
		agent.endActivityAndComputeNextState(100.0);
		sim.internalInterface.arrangeNextAgentState(agent);
		sim.getSimTimer().setTime(101.0);
		sim.doSimStep(); // agent should be moved to qlink2.buffer
		sim.getSimTimer().setTime(102.0);
		sim.doSimStep(); // agent should be moved to qlink3

		Collection<MobsimVehicle> vehicles = qlink3.getAllVehicles();
		Assert.assertEquals(1, vehicles.size());
		Assert.assertEquals(Id.create(2, Vehicle.class), vehicles.toArray(new MobsimVehicle[1])[0].getVehicle().getId());
		// vehicle 1 should still stay on qlink2
		vehicles = qlink2.getAllVehicles();
		Assert.assertEquals(1, vehicles.size());
		Assert.assertEquals(Id.create(1, Vehicle.class), vehicles.toArray(new MobsimVehicle[1])[0].getVehicle().getId());
	}

	/**
	 * Tests that a vehicle starts its route even when start and end link are the same.
	 *
	 * @author mrieser
	 */
	@Test
	public void testCircleAsRoute() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Link link4 = f.network.createAndAddLink(Id.create(4, Link.class), f.node4, f.node1, 1000.0, 100.0, 6000, 1.0); // close the network

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg(TransportMode.car);
		l1.setTravelTime(10);
		NetworkRoute netRoute = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link1.getId());
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		Collections.addAll(routeLinks, f.link2.getId(), f.link3.getId(), link4.getId());
		netRoute.setLinkIds(f.link1.getId(), routeLinks, f.link1.getId());
		l1.setRoute(netRoute);

		plan.createAndAddActivity("w", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		Assert.assertEquals("wrong number of events.", 16, allEvents.size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(4).getClass()); // link1
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(5).getClass()); // link2
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(7).getClass()); // link3
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(8).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(9).getClass()); // link4
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(10).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(11).getClass()); // link1 again
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(12).getClass());	
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(13).getClass());	
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(14).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(15).getClass());
	}

	/**
	 * Tests that if the endLink of a route is contained within the route itself,
	 * the vehicle really drives until the end of its route and is not stopped
	 * when it reaches the endLink the first time.
	 *
	 * @author mrieser
	 */
	@Test
	public void testRouteWithEndLinkTwice() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Link link4 = f.network.createAndAddLink(Id.create(4, Link.class), f.node4, f.node1, 1000.0, 100.0, 6000, 1.0); // close the network

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createAndAddLeg(TransportMode.car);
		l1.setTravelTime(10);
		NetworkRoute netRoute = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		Collections.addAll(routeLinks, f.link2.getId(), f.link3.getId(), link4.getId(), f.link1.getId(), f.link2.getId());
		netRoute.setLinkIds(f.link1.getId(), routeLinks, f.link3.getId());
		l1.setRoute(netRoute);

		plan.createAndAddActivity("w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		QSim sim = createQSim(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		Assert.assertEquals("wrong number of events.", 20, allEvents.size());
		Assert.assertEquals("wrong type of event.", ActivityEndEvent.class, allEvents.get(0).getClass());
		Assert.assertEquals("wrong type of event.", PersonDepartureEvent.class, allEvents.get(1).getClass());
		Assert.assertEquals("wrong type of event.", PersonEntersVehicleEvent.class, allEvents.get(2).getClass());
		Assert.assertEquals("wrong type of event.", VehicleEntersTrafficEvent.class, allEvents.get(3).getClass());
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(4).getClass()); // link1
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(5).getClass()); // link2
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(6).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(7).getClass()); // link3
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(8).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(9).getClass()); // link4
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(10).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(11).getClass()); // link1 again
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(12).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(13).getClass()); // link2 again
		Assert.assertEquals("wrong type of event.", LinkLeaveEvent.class, allEvents.get(14).getClass());
		Assert.assertEquals("wrong type of event.", LinkEnterEvent.class, allEvents.get(15).getClass()); // link3 again
		Assert.assertEquals("wrong type of event.", VehicleLeavesTrafficEvent.class, allEvents.get(16).getClass());
		Assert.assertEquals("wrong type of event.", PersonLeavesVehicleEvent.class, allEvents.get(17).getClass());
		Assert.assertEquals("wrong type of event.", PersonArrivalEvent.class, allEvents.get(18).getClass());
		Assert.assertEquals("wrong type of event.", ActivityStartEvent.class, allEvents.get(19).getClass());
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not lead to the destination link.
	 *
	 * @author mrieser
	 */
	@Test
	public void testConsistentRoutes_WrongRoute() {
		EventsManager events = EventsUtils.createEventsManager();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "5", events); // route should continue on link 4
		Assert.assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		Assert.assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	@Test
	public void testConsistentRoutes_WrongStartLink() {
		EventsManager events = EventsUtils.createEventsManager();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("2", "3 4", "5", events); // first act is on link 1, not 2
		Assert.assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		Assert.assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	@Test
	public void testConsistentRoutes_WrongEndLink() {
		EventsManager events = EventsUtils.createEventsManager();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "4", events); // second act is on link 5, not 4
		Assert.assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		Assert.assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not specify all nodes, so it is unclear at one node or another how to
	 * continue.
	 *
	 * @author mrieser
	 */
	@Test
	public void testConsistentRoutes_ImpossibleRoute() {
		EventsManager events = EventsUtils.createEventsManager();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 4", "5", events); // link 3 is missing
		Assert.assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		Assert.assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * is not specified, even that the destination link is different from the departure link.
	 *
	 * @author mrieser
	 */
	@Test
	public void testConsistentRoutes_MissingRoute() {
		EventsManager events = EventsUtils.createEventsManager();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "", "5", events); // no links at all
		Assert.assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		Assert.assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/** Prepares miscellaneous data for the testConsistentRoutes() tests:
	 * Creates a network of 6 links, and a population of one person driving from
	 * link 1 to link 5, and then from link 5 to link 6.
	 *
	 * @param startLinkId the start link of the route for the first leg
	 * @param linkIds the links the agent should travel along on the first leg
	 * @param endLinkId the end link of the route for the first leg
	 * @param events the Events object to be used by the simulation.
	 * @return A QueueSimulation which can be started immediately.
	 *
	 * @author mrieser
	 **/
	private LogCounter runConsistentRoutesTestSim(final String startLinkId, final String linkIds, final String endLinkId, final EventsManager events) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		/* enhance network */
		Node node5 = f.network.createAndAddNode(Id.create("5", Node.class), new Coord(3100, 0));
		Node node6 = f.network.createAndAddNode(Id.create("6", Node.class), new Coord(3200, 0));
		Node node7 = f.network.createAndAddNode(Id.create("7", Node.class), new Coord(3300, 0));
		f.network.createAndAddLink(Id.create("4", Link.class), f.node4, node5, 1000, 10, 6000, 2);
		Link link5 = f.network.createAndAddLink(Id.create("5", Link.class), node5, node6, 100, 10, 60000, 9);
		Link link6 = f.network.createAndAddLink(Id.create("6", Link.class), node6, node7, 100, 10, 60000, 9);

		((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).setRouteFactory(NetworkRoute.class, new LinkNetworkRouteFactory());

		// create a person with a car-leg from link1 to link5, but an incomplete route
		Person person = PopulationUtils.createPerson(Id.create(0, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", f.link1.getId());
		a1.setEndTime(8*3600);
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, f.link1.getId(), link5.getId());
		route.setLinkIds(Id.create(startLinkId, Link.class), NetworkUtils.getLinkIds(linkIds), Id.create(endLinkId, Link.class));
		leg.setRoute(route);
		ActivityImpl a2 = plan.createAndAddActivity("w", link5.getId());
		a2.setEndTime(9*3600);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = ((PopulationFactoryImpl) f.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, link5.getId(), link6.getId());
		route.setLinkIds(link5.getId(), null, link6.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("h", link6.getId());
		f.plans.addPerson(person);

		/* run sim with special logger */
		LogCounter logger = new LogCounter(Level.WARN);
		Logger.getRootLogger().addAppender(logger);
		createQSim(f, events).run();
		Logger.getRootLogger().removeAppender(logger);

		return logger;
	}

	@Test
	public void testStartAndEndTime() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();

		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
		
		// build simple network with 1 link
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000.0, 0.0));
		network.addNode(node1);
		network.addNode(node2);
		Link link = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
		link.setFreespeed(10.0);
		link.setCapacity(2000.0);
		network.addLink(link);

		// build simple population with 1 person with 1 plan with 1 leg
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		Activity act1 = pb.createActivityFromLinkId("h", link.getId());
		act1.setEndTime(7.0*3600);
		Leg leg = pb.createLeg(TransportMode.walk);
		Route route = new GenericRouteImpl(link.getId(), link.getId());
        route.setTravelTime(5.0*3600);
        leg.setRoute(route);
		Activity act2 = pb.createActivityFromLinkId("w", link.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
		person.addPlan(plan);
		population.addPerson(person);

		EventsManager events = EventsUtils.createEventsManager();
		FirstLastEventCollector collector = new FirstLastEventCollector();
		events.addHandler(collector);

		// first test without special settings
		QSim sim = createQSim(scenario, events);
		sim.run();
		Assert.assertEquals(act1.getEndTime(), collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals(act1.getEndTime() + leg.getRoute().getTravelTime(), collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
		collector.reset(0);

		// second test with special start/end times
		config.qsim().setStartTime(8.0*3600);
		config.qsim().setEndTime(11.0*3600);
		sim = createQSim(scenario, events);
		sim.run();
		Assert.assertEquals(8.0*3600, collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
		Assert.assertEquals(11.0*3600, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
	}

	/**
	 * Tests that cleanupSim() works correctly without generating NullPointerExceptions,
	 * even if there are still agents somewhere in the simulation.
	 *
	 * @author mrieser
	 */
	@Test
	public void testCleanupSim_EarlyEnd() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		
		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
		
		double simEndTime = 8.0*3600;

		// build simple network with 2 links
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000.0, 0.0));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(2000.0, 0.0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
		link1.setFreespeed(10.0); // freespeed-traveltime = 100s
		link1.setCapacity(2000.0);
		network.addLink(link1);
		Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
		link2.setFreespeed(10.0); // freespeed-traveltime = 100s
		link2.setCapacity(2000.0);
		network.addLink(link2);

		// build simple population with 3 persons with 1 plan with 1 leg
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		// person 1 : on the road when simulation ends
		Person person1 = pb.createPerson(Id.create("1", Person.class));
		Plan plan1 = pb.createPlan();
		Activity act1_1 = pb.createActivityFromLinkId("h", link1.getId());
		act1_1.setEndTime(simEndTime - 20);
		Leg leg1 = pb.createLeg(TransportMode.car);
		NetworkRoute route1 = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
		leg1.setRoute(route1);
		leg1.setTravelTime(5.0*3600);
		Activity act1_2 = pb.createActivityFromLinkId("w", link2.getId());
		plan1.addActivity(act1_1);
		plan1.addLeg(leg1);
		plan1.addActivity(act1_2);
		person1.addPlan(plan1);
		population.addPerson(person1);
		// person 2 : on teleportation when simulation ends
		Person person2 = pb.createPerson(Id.create("2", Person.class));
		Plan plan2 = pb.createPlan();
		Activity act2_1 = pb.createActivityFromLinkId("h", link1.getId());
		act2_1.setEndTime(simEndTime - 1000);
		Leg leg2 = pb.createLeg(TransportMode.walk);
		Route route2 = new GenericRouteImpl(link1.getId(), link2.getId());
		leg2.setRoute(route2);
		leg2.setTravelTime(2000);
		Activity act2_2 = pb.createActivityFromLinkId("w", link2.getId());
		plan2.addActivity(act2_1);
		plan2.addLeg(leg2);
		plan2.addActivity(act2_2);
		person2.addPlan(plan2);
		population.addPerson(person2);
		// person 3 : still at home when simulation ends
		Person person3 = pb.createPerson(Id.create("3", Person.class));
		Plan plan3 = pb.createPlan();
		Activity act3_1 = pb.createActivityFromLinkId("h", link1.getId());
		act3_1.setEndTime(simEndTime + 1000);
		Leg leg3 = pb.createLeg(TransportMode.walk);
		Route route3 = new GenericRouteImpl(link1.getId(), link2.getId());
		leg3.setRoute(route3);
		leg3.setTravelTime(1000);
		Activity act3_2 = pb.createActivityFromLinkId("w", link2.getId());
		plan3.addActivity(act3_1);
		plan3.addLeg(leg3);
		plan3.addActivity(act3_2);
		person3.addPlan(plan3);
		population.addPerson(person3);

		EventsManager events = EventsUtils.createEventsManager();
		FirstLastEventCollector collector = new FirstLastEventCollector();
		events.addHandler(collector);

		// run the simulation
		config.qsim().setEndTime(simEndTime);
		QSim sim = createQSim(scenario, events);
		sim.run();
		Assert.assertEquals(simEndTime, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
		// besides this, the important thing is that no (Runtime)Exception is thrown during this test
	}

	/**
	 * A simple events handler that counts the number of enter link events on one specific link.
	 * Used by some tests in the class.
	 *
	 * @author mrieser
	 */
	private final static class EnterLinkEventCounter implements LinkEnterEventHandler {
		private final String linkId;
		private int counter = 0;
		public EnterLinkEventCounter(final String linkId) {
			this.linkId = linkId;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			if (event.getLinkId().toString().equals(this.linkId)) this.counter++;
		}

		@Override
		public void reset(final int iteration) {
			this.counter = 0;
		}

		public int getCounter() {
			return this.counter;
		}
	}

	/*package*/ final static class FirstLastEventCollector implements BasicEventHandler {
		public Event firstEvent = null;
		public Event lastEvent = null;

		@Override
		public void handleEvent(final Event event) {
			if (firstEvent == null) {
				firstEvent = event;
			}
			lastEvent = event;
		}

		@Override
		public void reset(final int iteration) {
			firstEvent = null;
			lastEvent = null;
		}
	}

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	private static final class Fixture {
		final Config config;
		final Scenario scenario;
		final NetworkImpl network;
		final Node node1;
		final Node node2;
		final Node node3;
		final Node node4;
		final Link link1;
		final Link link2;
		final Link link3;
		final Population plans;
		final ArrayList<Id<Link>> linkIdsNone;
		final ArrayList<Id<Link>> linkIds2;

		public Fixture(boolean isUsingFastCapacityUpdate) {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.config = scenario.getConfig();
			this.config.qsim().setFlowCapFactor(1.0);
			this.config.qsim().setStorageCapFactor(1.0);
			
			this.config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

			/* build network */
			this.network = (NetworkImpl) this.scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			this.node1 = this.network.createAndAddNode(Id.create("1", Node.class), new Coord(0, 0));
			this.node2 = this.network.createAndAddNode(Id.create("2", Node.class), new Coord(100, 0));
			this.node3 = this.network.createAndAddNode(Id.create("3", Node.class), new Coord(1100, 0));
			this.node4 = this.network.createAndAddNode(Id.create("4", Node.class), new Coord(1200, 0));
			this.link1 = this.network.createAndAddLink(Id.create("1", Link.class), this.node1, this.node2, 100, 100, 60000, 9);
			this.link2 = this.network.createAndAddLink(Id.create("2", Link.class), this.node2, this.node3, 1000, 100, 6000, 2);
			this.link3 = this.network.createAndAddLink(Id.create("3", Link.class), this.node3, this.node4, 100, 100, 60000, 9);

			/* build plans */
			this.plans = scenario.getPopulation();

			this.linkIdsNone = new ArrayList<Id<Link>>();

			this.linkIds2 = new ArrayList<Id<Link>>();
			this.linkIds2.add(this.link2.getId());
		}
	}
}
