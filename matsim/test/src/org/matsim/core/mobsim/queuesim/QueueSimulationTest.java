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

package org.matsim.core.mobsim.queuesim;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

public class QueueSimulationTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(QueueSimulationTest.class);

	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	public void testSingleAgent() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createActivity("w", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 2, collector.events.size());
		assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 6.0*3600 + 12, collector.events.get(1).getTime(), EPSILON);
	}

	/**
	 * This test is mostly useful for manual debugging, because only two single agents are simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	public void testTwoAgent() {
		Fixture f = new Fixture();

		// add two persons with leg from link1 to link3, the first starting at 6am, the second at 7am
		for (int i = 0; i < 2; i++) {
			PersonImpl person = new PersonImpl(new IdImpl(i));
			PlanImpl plan = person.createPlan(true);
			ActivityImpl a1 = plan.createActivity("h", f.link1);
			a1.setEndTime((6+i)*3600);
			LegImpl leg = plan.createLeg(TransportMode.car);
			NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createActivity("w", f.link3);
			f.plans.getPersons().put(person.getId(), person);
		}

		/* build events */
		Events events = new Events();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 4, collector.events.size());
		assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 6.0*3600 + 12, collector.events.get(1).getTime(), EPSILON);
		assertEquals("wrong time in first event.", 7.0*3600 + 1, collector.events.get(2).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 7.0*3600 + 12, collector.events.get(3).getTime(), EPSILON);
	}

	/**
	 * A single agent is simulated that uses teleportation for its one and only leg.
	 *
	 * @author mrieser
	 */
	public void testTeleportationSingleAgent() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createLeg(TransportMode.other);
		RouteWRefs route = f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3); // TODO [MR] use different factory/mode here
		leg.setRoute(route);
		leg.setTravelTime(15.0);
		plan.createActivity("w", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		BasicEventCollector collector = new BasicEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of events.", 4, collector.events.size());
		assertEquals("wrong type of 1st event.", ActivityEndEvent.class, collector.events.get(0).getClass());
		assertEquals("wrong type of 2nd event.", AgentDepartureEvent.class, collector.events.get(1).getClass());
		assertEquals("wrong type of 3rd event.", AgentArrivalEvent.class, collector.events.get(2).getClass());
		assertEquals("wrong type of 4th event.", ActivityStartEvent.class, collector.events.get(3).getClass());
		assertEquals("wrong time in 1st event.", 6.0*3600 + 0, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in 2nd event.", 6.0*3600 + 0, collector.events.get(1).getTime(), EPSILON);
		assertEquals("wrong time in 3rd event.", 6.0*3600 + 15, collector.events.get(2).getTime(), EPSILON);
		assertEquals("wrong time in 4th event.", 6.0*3600 + 15, collector.events.get(3).getTime(), EPSILON);
	}

	/**
	 * Simulates a single agent that has two activities on the same link. Tests if the simulation
	 * correctly recognizes such cases.
	 *
	 * @author mrieser
	 */
	public void testSingleAgent_EmptyRoute() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link1);
		route.setNodes(f.link1, new ArrayList<Node>(0), f.link1);
		leg.setRoute(route);
		plan.createActivity("w", f.link1);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		BasicEventCollector collector = new BasicEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of events.", 4, collector.events.size());
		assertEquals("wrong type of 1st event.", ActivityEndEvent.class, collector.events.get(0).getClass());
		assertEquals("wrong type of 2nd event.", AgentDepartureEvent.class, collector.events.get(1).getClass());
		assertEquals("wrong type of 3rd event.", AgentArrivalEvent.class, collector.events.get(2).getClass());
		assertEquals("wrong type of 4th event.", ActivityStartEvent.class, collector.events.get(3).getClass());
		assertEquals("wrong time in 1st event.", 6.0*3600 + 0, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in 2nd event.", 6.0*3600 + 0, collector.events.get(1).getTime(), EPSILON);
		assertEquals("wrong time in 3rd event.", 6.0*3600 + 0, collector.events.get(2).getTime(), EPSILON);
		assertEquals("wrong time in 4th event.", 6.0*3600 + 0, collector.events.get(3).getTime(), EPSILON);
		assertEquals("wrong link in 1st event.", f.link1.getId(), ((ActivityEndEvent) collector.events.get(0)).getLinkId());
		assertEquals("wrong link in 2nd event.", f.link1.getId(), ((AgentDepartureEvent) collector.events.get(1)).getLinkId());
		assertEquals("wrong link in 3rd event.", f.link1.getId(), ((AgentArrivalEvent) collector.events.get(2)).getLinkId());
		assertEquals("wrong link in 4th event.", f.link1.getId(), ((ActivityStartEvent) collector.events.get(3)).getLinkId());
	}

	/*package*/ static class LinkEnterEventCollector implements BasicLinkEnterEventHandler {
		public final ArrayList<BasicLinkEnterEvent> events = new ArrayList<BasicLinkEnterEvent>();
		public void handleEvent(final BasicLinkEnterEvent event) {
			this.events.add(event);
		}
		public void reset(final int iteration) {
			this.events.clear();
		}
	}

	/*package*/ static class BasicEventCollector implements BasicEventHandler {
		public final ArrayList<BasicEvent> events = new ArrayList<BasicEvent>();
		public void handleEvent(final BasicEvent event) {
			this.events.add(event);
		}
		public void reset(final int iteration) {
			this.events.clear();
		}
	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all.
	 *
	 * @author mrieser
	 */
	public void testAgentWithoutLeg() {
		Fixture f = new Fixture();

		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createPlan(true);
		plan.createActivity("home", f.link1);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all, but the only activity has an end time set.
	 *
	 * @author mrieser
	 */
	public void testAgentWithoutLegWithEndtime() {
		Fixture f = new Fixture();

		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl act = plan.createActivity("home", f.link1);
		act.setEndTime(6.0 * 3600);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that no Exception occurs if the last activity of an agent has an end time set (which is wrong).
	 *
	 * @author mrieser
	 */
	public void testAgentWithLastActWithEndtime() {
		Fixture f = new Fixture();

		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl act = plan.createActivity("home", f.link1);
		act.setEndTime(6.0 * 3600);
		LegImpl leg = plan.createLeg(TransportMode.walk);
		leg.setRoute(new GenericRouteImpl(f.link1, f.link2));
		act = plan.createActivity("work", f.link2);
		act.setEndTime(6.0 * 3600 + 60);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityDriving() {
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createActivity("w", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		// add a lot of other persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
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
			ActivityImpl a = plan.createActivity("h", f.link1);
			a.setEndTime(7*3600 - 1812);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createActivity("w", f.link3);
			f.plans.getPersons().put(person.getId(), person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents starting on a link. Due to the different handling of these
	 * agents and their direct placing in the Buffer, it makes sense to
	 * test this specifically.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityStarting() {
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createActivity("w", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			ActivityImpl a2 = plan.createActivity("h", f.link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link2, f.nodes3, f.link3);
			leg.setRoute(route);
			plan.createActivity("w", f.link3);
			f.plans.getPersons().put(person.getId(), person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that the flow capacity of a link can be reached (but not exceeded) by
	 * agents starting on that link or driving through that link. This especially
	 * insures that the flow capacity measures both kinds (starting, driving) together.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityMixed() {
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createActivity("w", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 5000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			ActivityImpl a2 = plan.createActivity("h", f.link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link2, f.nodes3, f.link3);
			leg.setRoute(route);
			plan.createActivity("w", f.link3);
			f.plans.getPersons().put(person.getId(), person);
		}
		// add a lot of persons with legs from link1 to link3
		for (int i = 5001; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			ActivityImpl a2 = plan.createActivity("h", f.link1);
			a2.setEndTime(7*3600 - 1812);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createActivity("w", f.link3);
			f.plans.getPersons().put(person.getId(), person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that vehicles are teleported if needed so that agents can use the car wherever they want.
	 *
	 * @author mrieser
	 */
	public void testVehicleTeleportationTrue() {
		Fixture f = new Fixture();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createLeg(TransportMode.other);
		l1.setTravelTime(10);
		l1.setRoute(f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link2));
		ActivityImpl a2 = plan.createActivity("w", f.link2);
		a2.setEndTime(7.0*3600 + 20);
		LegImpl l2 = plan.createLeg(TransportMode.car);
		NetworkRoute route2 = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
		route2.setNodes(f.link2, f.nodes3, f.link3);
		l2.setRoute(route2);
		plan.createActivity("l", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		BasicEventCollector collector = new BasicEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.setTeleportVehicles(true);
		sim.run();

		/* finish */
		assertEquals("wrong number of events.", 11, collector.events.size());
		assertEquals("wrong type of event.", ActivityEndEvent.class, collector.events.get(0).getClass());
		assertEquals("wrong type of event.", AgentDepartureEvent.class, collector.events.get(1).getClass());
		assertEquals("wrong type of event.", AgentArrivalEvent.class, collector.events.get(2).getClass());
		assertEquals("wrong type of event.", ActivityStartEvent.class, collector.events.get(3).getClass());
		assertEquals("wrong type of event.", ActivityEndEvent.class, collector.events.get(4).getClass());
		assertEquals("wrong type of event.", AgentDepartureEvent.class, collector.events.get(5).getClass());
		assertEquals("wrong type of event.", AgentWait2LinkEvent.class, collector.events.get(6).getClass());
		assertEquals("wrong type of event.", LinkLeaveEvent.class, collector.events.get(7).getClass());
		assertEquals("wrong type of event.", LinkEnterEvent.class, collector.events.get(8).getClass());
		assertEquals("wrong type of event.", AgentArrivalEvent.class, collector.events.get(9).getClass());
		assertEquals("wrong type of event.", ActivityStartEvent.class, collector.events.get(10).getClass());
	}

	/**
	 * Tests that vehicles are not teleported if they are missing, but that an Exception is thrown instead.
	 *
	 * @author mrieser
	 */
	public void testVehicleTeleportationFalse() {
		Fixture f = new Fixture();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createLeg(TransportMode.other);
		l1.setTravelTime(10);
		l1.setRoute(f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link2)); // TODO [MR] use different factory / TransportationMode
		ActivityImpl a2 = plan.createActivity("w", f.link2);
		a2.setEndTime(7.0*3600 + 20);
		LegImpl l2 = plan.createLeg(TransportMode.car);
		NetworkRoute route2 = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
		route2.setNodes(f.link2, f.nodes3, f.link3);
		l2.setRoute(route2);
		plan.createActivity("l", f.link3);
		f.plans.getPersons().put(person.getId(), person);

		/* build events */
		Events events = new Events();
		BasicEventCollector collector = new BasicEventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.setTeleportVehicles(false);
		try {
			sim.run();
			fail("expected RuntimeException, but there was none.");
		} catch (RuntimeException e) {
			log.info("catched expected RuntimeException: " + e.getMessage());
		}

		/* finish */
		assertEquals("wrong number of events.", 6, collector.events.size());
		assertEquals("wrong type of event.", ActivityEndEvent.class, collector.events.get(0).getClass());
		assertEquals("wrong type of event.", AgentDepartureEvent.class, collector.events.get(1).getClass());
		assertEquals("wrong type of event.", AgentArrivalEvent.class, collector.events.get(2).getClass());
		assertEquals("wrong type of event.", ActivityStartEvent.class, collector.events.get(3).getClass());
		assertEquals("wrong type of event.", ActivityEndEvent.class, collector.events.get(4).getClass());
		assertEquals("wrong type of event.", AgentDepartureEvent.class, collector.events.get(5).getClass());
	}

	/**
	 * Tests that if a specific vehicle is assigned to an agent in its NetworkRoute, that this vehicle
	 * is used instead of a default one.
	 *
	 * @author mrieser
	 */
	public void testAssignedVehicles() {
		Fixture f = new Fixture();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		PersonImpl person = new PersonImpl(id1); // do not add person to population, we'll do it ourselves for the test
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link2);
		a1.setEndTime(7.0*3600);
		LegImpl l1 = plan.createLeg(TransportMode.car);
		NetworkRoute route1 = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
		route1.setNodes(f.link2, f.nodes3, f.link3);
		route1.setVehicleId(id2);
		l1.setRoute(route1);
		plan.createActivity("w", f.link3);

		/* build events */
		Events events = new Events();
		BasicEventCollector collector = new BasicEventCollector();
		events.addHandler(collector);

		/* prepare sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		QueueNetwork qnet = sim.getQueueNetwork();
		QueueLink qlink2 = qnet.getQueueLink(id2);
		QueueLink qlink3 = qnet.getQueueLink(new IdImpl(3));

		BasicVehicleType defaultVehicleType = new BasicVehicleTypeImpl(new IdImpl("defaultVehicleType"));
		QueueVehicle vehicle1 = new QueueVehicleImpl(new BasicVehicleImpl(id1, defaultVehicleType));
		QueueVehicle vehicle2 = new QueueVehicleImpl(new BasicVehicleImpl(id2, defaultVehicleType));
		qlink2.addParkedVehicle(vehicle1);
		qlink2.addParkedVehicle(vehicle2);

		SimulationTimer.setTime(100.0);
		PersonAgent agent = new PersonAgent(person, sim);
		agent.initialize();
		agent.activityEnds(100.0);

		SimulationTimer.setTime(101.0);
		sim.doSimStep(101.0); // agent should be moved to qlink2.buffer
		SimulationTimer.setTime(102.0);
		sim.doSimStep(102.0); // agent should be moved to qlink3

		Collection<QueueVehicle> vehicles = qlink3.getAllVehicles();
		assertEquals(1, vehicles.size());
		assertEquals(id2, vehicles.toArray(new QueueVehicle[1])[0].getBasicVehicle().getId());
		// vehicle 1 should still stay on qlink2
		vehicles = qlink2.getAllVehicles();
		assertEquals(1, vehicles.size());
		assertEquals(id1, vehicles.toArray(new QueueVehicle[1])[0].getBasicVehicle().getId());
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not lead to the destination link.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongRoute() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "5", events); // route should continue on link 4
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongStartLink() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("2", "3 4", "5", events); // first act is on link 1, not 2
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongEndLink() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "4", events); // second act is on link 5, not 4
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not specify all nodes, so it is unclear at one node or another how to
	 * continue.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_ImpossibleRoute() {
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 4", "5", events); // link 3 is missing
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * is not specified, even that the destination link is different from the departure link.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_MissingRoute() {
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "", "5", events); // no links at all
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
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
	private LogCounter runConsistentRoutesTestSim(final String startLinkId, final String linkIds, final String endLinkId, final Events events) {
		Fixture f = new Fixture();

		/* enhance network */
		NodeImpl node5 = f.network.createNode(new IdImpl("5"), new CoordImpl(3100, 0));
		NodeImpl node6 = f.network.createNode(new IdImpl("6"), new CoordImpl(3200, 0));
		NodeImpl node7 = f.network.createNode(new IdImpl("7"), new CoordImpl(3300, 0));
		f.network.createLink(new IdImpl("4"), f.node4, node5, 1000, 10, 6000, 2);
		LinkImpl link5 = f.network.createLink(new IdImpl("5"), node5, node6, 100, 10, 60000, 9);
		LinkImpl link6 = f.network.createLink(new IdImpl("6"), node6, node7, 100, 10, 60000, 9);

		// create a person with a car-leg from link1 to link5, but an incomplete route
		PersonImpl person = new PersonImpl(new IdImpl(0));
		PlanImpl plan = person.createPlan(true);
		ActivityImpl a1 = plan.createActivity("h", f.link1);
		a1.setEndTime(8*3600);
		LegImpl leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, link5);
		route.setLinks(f.network.getLink(new IdImpl(startLinkId)), NetworkUtils.getLinks(f.network, linkIds), f.network.getLink(new IdImpl(endLinkId)));
		leg.setRoute(route);
		ActivityImpl a2 = plan.createActivity("w", link5);
		a2.setEndTime(9*3600);
		leg = plan.createLeg(TransportMode.car);
		route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, link5, link6);
		route.setLinks(link5, null, link6);
		leg.setRoute(route);
		plan.createActivity("h", link6);
		f.plans.getPersons().put(person.getId(), person);

		/* run sim with special logger */
		LogCounter logger = new LogCounter();
		Logger.getRootLogger().addAppender(logger);
		new QueueSimulation(f.network, f.plans, events).run();
		Logger.getRootLogger().removeAppender(logger);

		return logger;
	}

	/**
	 * A simple events handler that counts the number of enter link events on one specific link.
	 * Used by some tests in the class.
	 *
	 * @author mrieser
	 */
	private final static class EnterLinkEventCounter implements BasicLinkEnterEventHandler {
		private final String linkId;
		private int counter = 0;
		public EnterLinkEventCounter(final String linkId) {
			this.linkId = linkId;
		}

		public void handleEvent(final BasicLinkEnterEvent event) {
			if (event.getLinkId().toString().equals(this.linkId)) this.counter++;
		}

		public void reset(final int iteration) {
			this.counter = 0;
		}

		public int getCounter() {
			return this.counter;
		}
	}

	private final static class LogCounter extends AppenderSkeleton {
		private int cntWARN = 0;
		private int cntERROR = 0;

		public LogCounter() {
			this.setThreshold(Level.WARN);
		}

		@Override
		protected void append(final LoggingEvent event) {
			if (event.getLevel() == Level.WARN) this.cntWARN++;
			if (event.getLevel() == Level.ERROR) this.cntERROR++;
		}

		public void close() {
		}

		public boolean requiresLayout() {
			return false;
		}

		public int getWarnCount() {
			return this.cntWARN;
		}

		public int getErrorCount() {
			return this.cntERROR;
		}
	}

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	private static final class Fixture {
		final Config config;
		final NetworkLayer network;
		final NodeImpl node1;
		final NodeImpl node2;
		final NodeImpl node3;
		final NodeImpl node4;
		final LinkImpl link1;
		final LinkImpl link2;
		final LinkImpl link3;
		final PopulationImpl plans;
		final ArrayList<Node> nodes3;
		final ArrayList<Node> nodes23;

		public Fixture() {
			this.config = Gbl.createConfig(null);
			this.config.simulation().setFlowCapFactor(1.0);
			this.config.simulation().setStorageCapFactor(1.0);

			/* build network */
			this.network = new NetworkLayer();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			this.node1 = this.network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
			this.node2 = this.network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
			this.node3 = this.network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
			this.node4 = this.network.createNode(new IdImpl("4"), new CoordImpl(1200, 0));
			this.link1 = this.network.createLink(new IdImpl("1"), this.node1, this.node2, 100, 100, 60000, 9);
			this.link2 = this.network.createLink(new IdImpl("2"), this.node2, this.node3, 1000, 100, 6000, 2);
			this.link3 = this.network.createLink(new IdImpl("3"), this.node3, this.node4, 100, 100, 60000, 9);

			/* build plans */
			this.plans = new PopulationImpl();

			this.nodes3 = new ArrayList<Node>();
			this.nodes3.add(this.node3);

			this.nodes23 = new ArrayList<Node>();
			this.nodes23.add(this.node2);
			this.nodes23.add(this.node3);
		}
	}
}
