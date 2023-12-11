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

package org.matsim.core.mobsim.hermes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
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
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.LogCounter;

public class HermesTest {

	private final static Logger log = LogManager.getLogger(HermesTest.class);

	protected static Hermes createHermes(MutableScenario scenario, EventsManager events) {
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		return new HermesBuilder().build(scenario, events);
	}

	protected static Hermes createHermes(Fixture f, EventsManager events) {
		return createHermes(f.scenario, events);
	}

	protected static Hermes createHermes(Scenario scenario, EventsManager events) {
		return createHermes(scenario, events, true);
	}

	protected static Hermes createHermes(Scenario scenario, EventsManager events, boolean prepareForSim) {
		if (prepareForSim) {
			PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		}
		return new HermesBuilder().build(scenario, events);
	}

	@BeforeEach
	public void prepareTest() {
		// TODO - fix these two!
		Id.resetCaches();
		ScenarioImporter.flush();
		HermesConfigGroup.SIM_STEPS = 30 * 60 * 60;
	}

	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	@Test
	void testSingleAgent() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		Assertions.assertEquals(2, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(6.0*3600, collector.events.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in first event.");
		Assertions.assertEquals(6.0*3600 + 11, collector.events.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in second event.");
	}


	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 * @author Kai Nagel
	 */
	@Test
	void testSingleAgentWithEndOnLeg() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		final PopulationFactory pf = f.scenario.getPopulation().getFactory();
		Person person = pf.createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		{
			Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a1.setEndTime(6*3600);
		}
		{
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		}
		{
			Activity act = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			act.setEndTime(6*3600);
		}
		{
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		}
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		createHermes(f, events).run();
//
//		/* finish */
		Assertions.assertEquals(2, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(6.0*3600, collector.events.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in first event.");
		Assertions.assertEquals(6.0*3600 + 11, collector.events.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in second event.");
	}

	/**
	 * This test is mostly useful for manual debugging, because only two single agents are simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	@Test
	void testTwoAgent() {
		Fixture f = new Fixture();

		// add two persons with leg from link1 to link3, the first starting at 6am, the second at 7am
		for (int i = 0; i < 2; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a1.setEndTime((6+i)*3600);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		Assertions.assertEquals(4, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(6.0*3600, collector.events.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in first event.");
		Assertions.assertEquals(6.0*3600 + 11, collector.events.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in second event.");
		Assertions.assertEquals(7.0*3600, collector.events.get(2).getTime(), MatsimTestUtils.EPSILON, "wrong time in first event.");
		Assertions.assertEquals(7.0*3600 + 11, collector.events.get(3).getTime(), MatsimTestUtils.EPSILON, "wrong time in second event.");
	}

	/**
	 * A single agent is simulated that uses teleportation for its one and only leg.
	 *
	 * @author mrieser
	 */
	@Test
	void testTeleportationSingleAgent() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg( plan, "other" );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		Route route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(Route.class, f.link1.getId(), f.link3.getId()); // TODO [MR] use different factory/mode here
		route.setTravelTime(15.0);
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		List<Event> allEvents = collector.getEvents();
		Assertions.assertEquals(5, collector.getEvents().size(), "wrong number of events.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of event.");
		Assertions.assertEquals(TeleportationArrivalEvent.class, allEvents.get(2).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(3).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(4).getClass(), "wrong type of event.");
		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in event.");
		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in event.");
		Assertions.assertEquals(6.0 * 3600 + 13, allEvents.get(2).getTime(), MatsimTestUtils.EPSILON, "wrong time in event.");
		Assertions.assertEquals(6.0 * 3600 + 13, allEvents.get(3).getTime(), MatsimTestUtils.EPSILON, "wrong time in event.");
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
	void testSingleAgentImmediateDeparture() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(0);
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		HermesConfigGroup.SIM_STEPS = 10 * 3600;
		sim.run();

		/* finish */
		Assertions.assertEquals(2, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(0.0*3600, collector.events.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in first event.");
		Assertions.assertEquals(0.0*3600 + 11, collector.events.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in second event.");
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
	void testSingleAgent_EmptyRoute() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link1
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link1.getId());
		route.setLinkIds(f.link1.getId(), new ArrayList<Id<Link>>(0), f.link1.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}
		Assertions.assertEquals(8, allEvents.size(), "wrong number of events.");


		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of 1st event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of 2nd event.");
		Assertions.assertEquals(PersonEntersVehicleEvent.class, allEvents.get(2).getClass(), "wrong type of 3rd event.");
		Assertions.assertEquals(VehicleEntersTrafficEvent.class, allEvents.get(3).getClass(), "wrong type of 4th event.");
		Assertions.assertEquals(VehicleLeavesTrafficEvent.class, allEvents.get(4).getClass(), "wrong type of 5th event.");
		Assertions.assertEquals(PersonLeavesVehicleEvent.class, allEvents.get(5).getClass(), "wrong type of 6th event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(6).getClass(), "wrong type of 7th event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(7).getClass(), "wrong type of 8th event.");


		Assertions.assertEquals(6.0*3600 + 0, allEvents.get(0).getTime(), MatsimTestUtils.EPSILON, "wrong time in 1st event.");
		Assertions.assertEquals(6.0*3600 + 0, allEvents.get(1).getTime(), MatsimTestUtils.EPSILON, "wrong time in 2nd event.");
		Assertions.assertEquals(6.0*3600 + 0, allEvents.get(2).getTime(), MatsimTestUtils.EPSILON, "wrong time in 3rd event.");
		Assertions.assertEquals(6.0*3600 + 0, allEvents.get(3).getTime(), MatsimTestUtils.EPSILON, "wrong time in 4th event.");

		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(4).getTime(),
				MatsimTestUtils.EPSILON,
				"wrong time in 5th event.");
		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(5).getTime(),
				MatsimTestUtils.EPSILON,
				"wrong time in 6th event.");
		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(6).getTime(),
				MatsimTestUtils.EPSILON,
				"wrong time in 7th event.");
		Assertions.assertEquals(6.0 * 3600 + 0, allEvents.get(7).getTime(),
				MatsimTestUtils.EPSILON,
				"wrong time in 8th event.");


		Assertions.assertEquals(f.link1.getId(), ((ActivityEndEvent) allEvents.get(0)).getLinkId(), "wrong link in 1st event." );
		Assertions.assertEquals(f.link1.getId(), ((PersonDepartureEvent) allEvents.get(1)).getLinkId(), "wrong link in 2nd event." );
		Assertions.assertEquals(f.link1.getId(), ((VehicleEntersTrafficEvent) allEvents.get(3)).getLinkId(), "wrong link in 4th event." );
		Assertions.assertEquals(f.link1.getId(), ((VehicleLeavesTrafficEvent) allEvents.get(4)).getLinkId(), "wrong link in 5th event." );
		Assertions.assertEquals(f.link1.getId(), ((PersonArrivalEvent) allEvents.get(6)).getLinkId(), "wrong link in 7th event." );
		Assertions.assertEquals(f.link1.getId(), ((ActivityStartEvent) allEvents.get(7)).getLinkId(), "wrong link in 8th event." );
	}

	/**
	 * Simulates a single agent whose route ends on a link that is actually a loop link.
	 * Yes, this can happen in real scenarios.
	 *
	 * @author mrieser
	 */
	@Test
	void testSingleAgent_LastLinkIsLoop() {
		Fixture f = new Fixture();
		Link loopLink = NetworkUtils.createAndAddLink(f.network,Id.create("loop", Link.class), f.node4, f.node4, 100.0, 10.0, 500, 1 );

		// add a single person with leg from link1 to loop-link
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), loopLink.getId());
		ArrayList<Id<Link>> links = new ArrayList<Id<Link>>();
		links.add(f.link2.getId());
		links.add(f.link3.getId());
		route.setLinkIds(f.link1.getId(), links, loopLink.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", loopLink.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		f.config.qsim().setEndTime(7*3600);
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}
		Assertions.assertEquals(14, allEvents.size(), "wrong number of events.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of 1st event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of 2nd event.");
		Assertions.assertEquals(PersonEntersVehicleEvent.class, allEvents.get(2).getClass(), "wrong type of event.");
		Assertions.assertEquals(VehicleEntersTrafficEvent.class, allEvents.get(3).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(4).getClass(), "wrong type of event."); // link 1
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(5).getClass(), "wrong type of event."); // link 2
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(6).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(7).getClass(), "wrong type of event."); // link 3
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(8).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(9).getClass(), "wrong type of event."); // loop link
		Assertions.assertEquals(VehicleLeavesTrafficEvent.class, allEvents.get(10).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonLeavesVehicleEvent.class, allEvents.get(11).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(12).getClass(), "wrong type of 11th event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(13).getClass(), "wrong type of 12th event.");
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
	void testAgentWithoutLeg() {
		Fixture f = new Fixture();

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		Assertions.assertEquals(0, collector.events.size(), "wrong number of link enter events.");
	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all, but the only activity has an end time set.
	 *
	 * @author mrieser
	 */
	@Test
	void testAgentWithoutLegWithEndtime() {
		Fixture f = new Fixture();

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", f.link1.getId());
		act.setEndTime(6.0 * 3600);
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		Assertions.assertEquals(0, collector.events.size(), "wrong number of link enter events.");
	}

	/**
	 * Tests that no Exception occurs if the last activity of an agent has an end time set (which is wrong).
	 *
	 * @author mrieser
	 */
	@Test
	void testAgentWithLastActWithEndtime() {
		Fixture f = new Fixture();

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", f.link1.getId());
		act.setEndTime(6.0 * 3600);
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		TripStructureUtils.setRoutingMode( leg, TransportMode.car );
		leg.setRoute(RouteUtils.createGenericRouteImpl(f.link1.getId(), f.link2.getId()));
		leg.getRoute().setTravelTime(0.); // retrofitting to repair failing test.  kai, apr'15
		act = PopulationUtils.createAndAddActivityFromLinkId(plan, "work", f.link2.getId());
		act.setEndTime(6.0 * 3600 + 60);
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		Assertions.assertEquals(0, collector.events.size(), "wrong number of link enter events.");
	}

	/**
	 * Tests that vehicles are teleported if needed so that agents can use the car wherever they want.
	 *
	 * @author mrieser
	 */
	@Test
	void testVehicleTeleportationTrue() {
		Fixture f = new Fixture();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		Leg l1 = PopulationUtils.createAndAddLeg( plan, TransportMode.other );
		TripStructureUtils.setRoutingMode( l1, TransportMode.other );
		l1.setTravelTime(10);
		l1.setRoute(f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(GenericRouteImpl.class, f.link1.getId(), f.link2.getId()));
		Activity a2 = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link2.getId());
		a2.setEndTime(7.0*3600 + 20);
		Leg l2 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( l2, TransportMode.car );
		NetworkRoute route2 = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link2.getId(), f.link3.getId());
		route2.setLinkIds(f.link2.getId(), f.linkIdsNone, f.link3.getId());
		l2.setRoute(route2);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "l", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		List<Event> allEvents = collector.getEvents();
		Assertions.assertEquals(15, allEvents.size(), "wrong number of events.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of event.");
		Assertions.assertEquals(TeleportationArrivalEvent.class, allEvents.get(2).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(3).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(4).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(5).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(6).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonEntersVehicleEvent.class, allEvents.get(7).getClass(), "wrong type of event.");
		Assertions.assertEquals(VehicleEntersTrafficEvent.class, allEvents.get(8).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(9).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(10).getClass(), "wrong type of event.");
		Assertions.assertEquals(VehicleLeavesTrafficEvent.class, allEvents.get(11).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonLeavesVehicleEvent.class, allEvents.get(12).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(13).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(14).getClass(), "wrong type of event.");
	}

	/**
	 * Tests that a vehicle starts its route even when start and end link are the same.
	 *
	 * @author mrieser
	 */
	@Test
	void testCircleAsRoute() {
		Fixture f = new Fixture();
		Link link4 = NetworkUtils.createAndAddLink(f.network,Id.create(4, Link.class), f.node4, f.node1, 1000.0, 100.0, 6000, 1.0 ); // close the network

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		Leg l1 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( l1, TransportMode.car );
		l1.setTravelTime(10);
		NetworkRoute netRoute = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link1.getId());
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		Collections.addAll(routeLinks, f.link2.getId(), f.link3.getId(), link4.getId());
		netRoute.setLinkIds(f.link1.getId(), routeLinks, f.link1.getId());
		l1.setRoute(netRoute);

		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link1.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}

		Assertions.assertEquals(16, allEvents.size(), "wrong number of events.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonEntersVehicleEvent.class, allEvents.get(2).getClass(), "wrong type of event.");
		Assertions.assertEquals(VehicleEntersTrafficEvent.class, allEvents.get(3).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(4).getClass(), "wrong type of event."); // link1
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(5).getClass(), "wrong type of event."); // link2
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(6).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(7).getClass(), "wrong type of event."); // link3
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(8).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(9).getClass(), "wrong type of event."); // link4
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(10).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(11).getClass(), "wrong type of event."); // link1 again
		Assertions.assertEquals(VehicleLeavesTrafficEvent.class, allEvents.get(12).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonLeavesVehicleEvent.class, allEvents.get(13).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(14).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(15).getClass(), "wrong type of event.");
	}

	/**
	 * Tests that if the endLink of a route is contained within the route itself,
	 * the vehicle really drives until the end of its route and is not stopped
	 * when it reaches the endLink the first time.
	 *
	 * @author mrieser
	 */
	@Test
	void testRouteWithEndLinkTwice() {
		Fixture f = new Fixture();
		Link link4 = NetworkUtils.createAndAddLink(f.network,Id.create(4, Link.class), f.node4, f.node1, 1000.0, 100.0, 6000, 1.0 ); // close the network

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		Leg l1 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode( l1, TransportMode.car );
		l1.setTravelTime(10);
		NetworkRoute netRoute = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		Collections.addAll(routeLinks, f.link2.getId(), f.link3.getId(), link4.getId(), f.link1.getId(), f.link2.getId());
		netRoute.setLinkIds(f.link1.getId(), routeLinks, f.link3.getId());
		l1.setRoute(netRoute);

		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		List<Event> allEvents = collector.getEvents();
		for (Event event : allEvents) {
			System.out.println(event);
		}

		Assertions.assertEquals(20, allEvents.size(), "wrong number of events.");
		Assertions.assertEquals(ActivityEndEvent.class, allEvents.get(0).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonDepartureEvent.class, allEvents.get(1).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonEntersVehicleEvent.class, allEvents.get(2).getClass(), "wrong type of event.");
		Assertions.assertEquals(VehicleEntersTrafficEvent.class, allEvents.get(3).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(4).getClass(), "wrong type of event."); // link1
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(5).getClass(), "wrong type of event."); // link2
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(6).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(7).getClass(), "wrong type of event."); // link3
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(8).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(9).getClass(), "wrong type of event."); // link4
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(10).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(11).getClass(), "wrong type of event."); // link1 again
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(12).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(13).getClass(), "wrong type of event."); // link2 again
		Assertions.assertEquals(LinkLeaveEvent.class, allEvents.get(14).getClass(), "wrong type of event.");
		Assertions.assertEquals(LinkEnterEvent.class, allEvents.get(15).getClass(), "wrong type of event."); // link3 again
		Assertions.assertEquals(VehicleLeavesTrafficEvent.class, allEvents.get(16).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonLeavesVehicleEvent.class, allEvents.get(17).getClass(), "wrong type of event.");
		Assertions.assertEquals(PersonArrivalEvent.class, allEvents.get(18).getClass(), "wrong type of event.");
		Assertions.assertEquals(ActivityStartEvent.class, allEvents.get(19).getClass(), "wrong type of event.");
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
		Fixture f = new Fixture();

		/* enhance network */
		Node node5 = NetworkUtils.createAndAddNode(f.network, Id.create("5", Node.class), new Coord(3100, 0));
		Node node6 = NetworkUtils.createAndAddNode(f.network, Id.create("6", Node.class), new Coord(3200, 0));
		Node node7 = NetworkUtils.createAndAddNode(f.network, Id.create("7", Node.class), new Coord(3300, 0));
		final Node toNode = node5;
		NetworkUtils.createAndAddLink(f.network, Id.create("4", Link.class), f.node4, toNode, 1000, 10, 6000, 2);
		final Node fromNode = node5;
		final Node toNode1 = node6;
		Link link5 = NetworkUtils.createAndAddLink(f.network, Id.create("5", Link.class), fromNode, toNode1, 100, 10, 60000, 9);
		final Node fromNode1 = node6;
		final Node toNode2 = node7;
		Link link6 = NetworkUtils.createAndAddLink(f.network, Id.create("6", Link.class), fromNode1, toNode2, 100, 10, 60000, 9);

		f.scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(NetworkRoute.class, new LinkNetworkRouteFactory());

		// create a person with a car-leg from link1 to link5, but an incomplete route
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(8 * 3600);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
		TripStructureUtils.setRoutingMode(leg, TransportMode.car);
		NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), link5.getId());
		route.setLinkIds(Id.create(startLinkId, Link.class), NetworkUtils.getLinkIds(linkIds), Id.create(endLinkId, Link.class));
		leg.setRoute(route);
		Activity a2 = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link5.getId());
		a2.setEndTime(9 * 3600);
		leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
		TripStructureUtils.setRoutingMode(leg, TransportMode.car);
		route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, link5.getId(), link6.getId());
		route.setLinkIds(link5.getId(), null, link6.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link6.getId());
		f.plans.addPerson(person);

		/* run sim with special logger */
		LogCounter logger = new LogCounter(Level.WARN);
		logger.activate();
		createHermes(f, events).run();
		logger.deactivate();

		return logger;
	}

	@Test
	void testStartAndEndTime() {

		final Config config = ConfigUtils.createConfig();

		// ---

		MutableScenario scenario = ScenarioUtils.createMutableScenario( config );

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
		TripStructureUtils.setRoutingMode( leg, TransportMode.walk );
		Route route = RouteUtils.createGenericRouteImpl(link.getId(), link.getId());
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

		Hermes sim = createHermes(scenario, events);
		HermesConfigGroup.SIM_STEPS = 11 * 3600;
		sim.run();
		Assertions.assertEquals(7.0*3600, collector.firstEvent.getTime(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(11.0*3600, collector.lastEvent.getTime(), MatsimTestUtils.EPSILON);
	}

	/**
	 * Tests that cleanupSim() works correctly without generating NullPointerExceptions,
	 * even if there are still agents somewhere in the simulation.
	 *
	 * @author mrieser
	 */
	@Test
	void testCleanupSim_EarlyEnd() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();

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
		TripStructureUtils.setRoutingMode( leg1, TransportMode.car );
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link2.getId());
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
		TripStructureUtils.setRoutingMode( leg2, TransportMode.walk );
		Route route2 = RouteUtils.createGenericRouteImpl(link1.getId(), link2.getId());
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
		TripStructureUtils.setRoutingMode( leg3, TransportMode.walk );
		Route route3 = RouteUtils.createGenericRouteImpl(link1.getId(), link2.getId());
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
		Hermes sim = createHermes(scenario, events);
		HermesConfigGroup.SIM_STEPS = (int) simEndTime;
		sim.run();
		Assertions.assertEquals(simEndTime, collector.lastEvent.getTime(), MatsimTestUtils.EPSILON);
		// besides this, the important thing is that no (Runtime)Exception is thrown during this test
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
	public static final class Fixture {
		final Config config;
		final Scenario scenario;
		final Network network;
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

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.config = scenario.getConfig();


			/* build network */
			this.network = this.scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			this.node1 = NetworkUtils.createAndAddNode(this.network, Id.create("1", Node.class), new Coord(0, 0));
			this.node2 = NetworkUtils.createAndAddNode(this.network, Id.create("2", Node.class), new Coord(100, 0));
			this.node3 = NetworkUtils.createAndAddNode(this.network, Id.create("3", Node.class), new Coord(1100, 0));
			this.node4 = NetworkUtils.createAndAddNode(this.network, Id.create("4", Node.class), new Coord(1200, 0));
			this.link1 = NetworkUtils.createAndAddLink(this.network,Id.create("1", Link.class), this.node1, this.node2, 100, 100, 60000, 9 );
			this.link2 = NetworkUtils.createAndAddLink(this.network,Id.create("2", Link.class), this.node2, this.node3, 1000, 100, 6000, 2 );
			this.link3 = NetworkUtils.createAndAddLink(this.network,Id.create("3", Link.class), this.node3, this.node4, 100, 100, 60000, 9 );

			/* build plans */
			this.plans = scenario.getPopulation();

			this.linkIdsNone = new ArrayList<Id<Link>>();

			this.linkIds2 = new ArrayList<Id<Link>>();
			this.linkIds2.add(this.link2.getId());
		}
	}
}
