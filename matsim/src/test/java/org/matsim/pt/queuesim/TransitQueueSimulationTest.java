/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.queuesim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.qsim.SimpleTransitStopHandler;
import org.matsim.pt.qsim.TransitDriver;
import org.matsim.pt.qsim.TransitDriverAgent;
import org.matsim.pt.qsim.TransitQSimulation;
import org.matsim.pt.qsim.TransitQVehicle;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.qsim.TransitQSimFeature.TransitAgentTriesToTeleportException;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.Simulation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.VehiclesFactory;


/**
 * @author mrieser
 */
public class TransitQueueSimulationTest {

	/**
	 * Ensure that for each departure an agent is created and departs
	 */
	@Test
	public void testCreateAgents() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setEndTime(8.0*3600);

		// setup: network
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		Node node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(2000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		setDefaultLinkAttributes(link1);
		Link link2 = network.getFactory().createLink(scenario.createId("2"), node2.getId(), node3.getId());
		setDefaultLinkAttributes(link2);
		network.addLink(link1);
		network.addLink(link2);

		// setup: vehicles
		BasicVehicles vehicles = scenario.getVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		BasicVehicleType vehicleType = vb.createVehicleType(new IdImpl("transitVehicleType"));
		BasicVehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		vehicles.getVehicles().put(new IdImpl("veh1"), vb.createVehicle(new IdImpl("veh1"), vehicleType));
		vehicles.getVehicles().put(new IdImpl("veh2"), vb.createVehicle(new IdImpl("veh2"), vehicleType));
		vehicles.getVehicles().put(new IdImpl("veh3"), vb.createVehicle(new IdImpl("veh3"), vehicleType));
		vehicles.getVehicles().put(new IdImpl("veh4"), vb.createVehicle(new IdImpl("veh4"), vehicleType));
		vehicles.getVehicles().put(new IdImpl("veh5"), vb.createVehicle(new IdImpl("veh5"), vehicleType));

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("stop3"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(scenario.createId("stop4"), scenario.createCoord(0, 0), false);
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);
		stop1.setLinkId(link1.getId());
		stop2.setLinkId(link1.getId());
		stop3.setLinkId(link2.getId());
		stop4.setLinkId(link2.getId());

		NetworkRouteWRefs route = new NodeNetworkRouteImpl(link1.getId(), link2.getId(), network);
		ArrayList<Id> links = new ArrayList<Id>(0);
		route.setLinkIds(link1.getId(), links, link2.getId());

		{ // line 1, 1 route, 2 departures
			TransitLine line = builder.createTransitLine(scenario.createId("1"));
			TransitRoute tRoute = builder.createTransitRoute(scenario.createId(">"), route, stops, TransportMode.pt);
			Departure dep = builder.createDeparture(scenario.createId("dep1"), 6.0*3600);
			dep.setVehicleId(new IdImpl("veh1"));
			tRoute.addDeparture(dep);
			dep = builder.createDeparture(scenario.createId("dep2"), 7.0*3600);
			dep.setVehicleId(new IdImpl("veh2"));
			tRoute.addDeparture(dep);
			line.addRoute(tRoute);
			schedule.addTransitLine(line);
		}

		{ // line 2, 3 routes, each 1 departure
			TransitLine line = builder.createTransitLine(scenario.createId("2"));
			{ // route 1
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("A"), route, stops, TransportMode.pt);
				Departure dep = builder.createDeparture(scenario.createId("dep3"), 8.0*3600);
				dep.setVehicleId(new IdImpl("veh3"));
				tRoute.addDeparture(dep);
				line.addRoute(tRoute);
			}
			{ // route 2
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("B"), route, stops, TransportMode.pt);
				Departure dep = builder.createDeparture(scenario.createId("dep4"), 8.5*3600);
				dep.setVehicleId(new IdImpl("veh4"));
				tRoute.addDeparture(dep);
				line.addRoute(tRoute);
			}
			{ // route 3
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("C"), route, stops, TransportMode.pt);
				Departure dep = builder.createDeparture(scenario.createId("dep5"), 9.0*3600);
				dep.setVehicleId(new IdImpl("veh5"));
				tRoute.addDeparture(dep);
				line.addRoute(tRoute);
			}
			schedule.addTransitLine(line);
		}

		scenario.getConfig().simulation().setEndTime(1.0*3600); // prevent running the actual simulation
		TestCreateAgentsSimulation sim = new TestCreateAgentsSimulation(scenario, new EventsManagerImpl());
		sim.run();
		List<DriverAgent> agents = sim.createdAgents;
		assertEquals(5, agents.size());
		assertTrue(agents.get(0) instanceof TransitDriverAgent);
		assertEquals(6.0*3600, ((TransitDriverAgent) agents.get(0)).getDepartureTime(), MatsimTestCase.EPSILON);
		assertEquals(7.0*3600, ((TransitDriverAgent) agents.get(1)).getDepartureTime(), MatsimTestCase.EPSILON);
		assertEquals(8.0*3600, ((TransitDriverAgent) agents.get(2)).getDepartureTime(), MatsimTestCase.EPSILON);
		assertEquals(8.5*3600, ((TransitDriverAgent) agents.get(3)).getDepartureTime(), MatsimTestCase.EPSILON);
		assertEquals(9.0*3600, ((TransitDriverAgent) agents.get(4)).getDepartureTime(), MatsimTestCase.EPSILON);
	}

	protected static class TestCreateAgentsSimulation extends TransitQSimulation {
		public final List<DriverAgent> createdAgents = new ArrayList<DriverAgent>();
		public TestCreateAgentsSimulation(final ScenarioImpl scenario, final EventsManagerImpl events) {
			super(scenario, events);
		}
		@Override
		protected void createAgents() {
			super.createAgents();
			this.createdAgents.addAll(super.activityEndsList);
		}
	}

	/**
	 * Tests that the simulation is adding an agent correctly to the transit stop
	 */
	@Test
	public void testAddAgentToStop() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		// setup: network
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		network.addNode(node1);
		network.addNode(node2);
		Link link = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		setDefaultLinkAttributes(link);
		network.addLink(link);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitLine line = builder.createTransitLine(scenario.createId("1"));

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0), false);
		stop1.setLinkId(link.getId());
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0), false);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);

		// setup: population
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));

		homeAct.setEndTime(7.0*3600 - 10.0);
		// as no transit line runs, make sure to stop the simulation manually.
		scenario.getConfig().getQSimConfigGroup().setEndTime(7.0*3600);

		Leg leg = pb.createLeg(TransportMode.pt);
		leg.setRoute(new ExperimentalTransitRoute(stop1, line, null, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);
		population.addPerson(person);

		// run simulation
		EventsManagerImpl events = new EventsManagerImpl();
		TransitQSimulation simulation = new TransitQSimulation(scenario, events);
		simulation.run();

		// check everything
		assertEquals(1, simulation.getAgentTracker().getAgentsAtStop(stop1).size());
	}

	/**
	 * Tests that the simulation refuses to let an agent teleport herself by starting a transit
	 * leg on a link where she isn't.
	 *
	 */
	@Test(expected = TransitAgentTriesToTeleportException.class)
	public void testAddAgentToStopWrongLink() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		// setup: network
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		Node node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(2000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		Link link2 = network.getFactory().createLink(scenario.createId("2"), node2.getId(), node3.getId());
		setDefaultLinkAttributes(link1);
		network.addLink(link1);
		setDefaultLinkAttributes(link2);
		network.addLink(link2);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitLine line = builder.createTransitLine(scenario.createId("1"));

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0), false);
		stop1.setLinkId(link1.getId());
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0), false);
		stop2.setLinkId(link2.getId());
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);

		// setup: population
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("2"));

		homeAct.setEndTime(7.0*3600 - 10.0);
		// as no transit line runs, make sure to stop the simulation manually.
		scenario.getConfig().getQSimConfigGroup().setEndTime(7.0*3600);

		Leg leg = pb.createLeg(TransportMode.pt);
		leg.setRoute(new ExperimentalTransitRoute(stop1, line, null, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("1"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);
		population.addPerson(person);

		// run simulation
		EventsManagerImpl events = new EventsManagerImpl();
		TransitQSimulation simulation = new TransitQSimulation(scenario, events);
		simulation.run();
	}

	/**
	 * Tests that a vehicle's handleStop() method is correctly called, e.g.
	 * it is re-called again when returning a delay > 0, and that is is correctly
	 * called when the stop is located on the first link of the network route, on the last
	 * link of the network route, or any intermediary link.
	 */
	@Test
	public void testHandleStop() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setEndTime(8.0*3600);

		// setup: network
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		Node node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(2000, 0));
		Node node4 = network.getFactory().createNode(scenario.createId("4"), scenario.createCoord(3000, 0));
		Node node5 = network.getFactory().createNode(scenario.createId("5"), scenario.createCoord(4000, 0));
		Node node6 = network.getFactory().createNode(scenario.createId("6"), scenario.createCoord(5000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
		Link link1 = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		Link link2 = network.getFactory().createLink(scenario.createId("2"), node2.getId(), node3.getId());
		Link link3 = network.getFactory().createLink(scenario.createId("3"), node3.getId(), node4.getId());
		Link link4 = network.getFactory().createLink(scenario.createId("4"), node4.getId(), node5.getId());
		Link link5 = network.getFactory().createLink(scenario.createId("5"), node5.getId(), node6.getId());
		setDefaultLinkAttributes(link1);
		setDefaultLinkAttributes(link2);
		setDefaultLinkAttributes(link3);
		setDefaultLinkAttributes(link4);
		setDefaultLinkAttributes(link5);
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitLine line = builder.createTransitLine(scenario.createId("1"));
		// important: do NOT add the line to the schedule, or agents will be created twice!

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("stop3"), scenario.createCoord(0, 0), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(scenario.createId("stop4"), scenario.createCoord(0, 0), false);
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);

		stop1.setLinkId(link1.getId()); // one stop on the first link of network route, as that one may be specially handled
		stop2.setLinkId(link3.getId()); // some stop in the middle of the network route
		stop3.setLinkId(link4.getId());
		stop4.setLinkId(link5.getId()); // one stop on the last link of the network route, as that one may be specially handled

		NetworkRouteWRefs route = new NodeNetworkRouteImpl(link1.getId(), link5.getId(), network);
		ArrayList<Id> links = new ArrayList<Id>();
		Collections.addAll(links, link2.getId(), link3.getId(), link4.getId());
		route.setLinkIds(link1.getId(), links, link5.getId());

		TransitRoute tRoute = builder.createTransitRoute(scenario.createId(">"), route, stops, TransportMode.pt);
		Departure departure = builder.createDeparture(scenario.createId("dep1"), 6.0*3600);
		tRoute.addDeparture(departure);
		line.addRoute(tRoute);

		// setup: population
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		Person person1 = pb.createPerson(scenario.createId("1"));
		Plan plan1 = pb.createPlan();
		person1.addPlan(plan1);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		homeAct.setEndTime(departure.getDepartureTime() - 60.0);
		Leg leg1 = pb.createLeg(TransportMode.pt);
		leg1.setRoute(new ExperimentalTransitRoute(stop1, line, tRoute, stop3));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan1.addActivity(homeAct);
		plan1.addLeg(leg1);
		plan1.addActivity(workAct);
		population.addPerson(person1);

		Person person2 = pb.createPerson(scenario.createId("2"));
		Plan plan2 = pb.createPlan();
		person2.addPlan(plan2);
		Leg leg2 = pb.createLeg(TransportMode.pt);
		leg2.setRoute(new ExperimentalTransitRoute(stop3, line, tRoute, stop4));
		Activity homeActOnLink4 = pb.createActivityFromLinkId("home", scenario.createId("4"));
		homeActOnLink4.setEndTime(departure.getDepartureTime() - 60.0);
		plan2.addActivity(homeActOnLink4);
		plan2.addLeg(leg2);
		Activity workActOnLink5 = pb.createActivityFromLinkId("work", scenario.createId("5"));
		plan2.addActivity(workActOnLink5);
		population.addPerson(person2);


		// run simulation
		EventsManagerImpl events = new EventsManagerImpl();
		TestHandleStopSimulation simulation = new TestHandleStopSimulation(scenario, events, line, tRoute, departure);
		simulation.run();

		// check everything
		List<SpyHandleStopData> spyData = simulation.driver.spyData;
		assertEquals(7, spyData.size());
		SpyHandleStopData data;

		data = spyData.get(0);
		assertEquals(stop1, data.stopFacility);
		assertTrue(data.returnedDelay > 0);
		double lastTime = data.time;
		double lastDelay = data.returnedDelay;

		data = spyData.get(1);
		assertEquals(stop1, data.stopFacility);
		assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
		assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);

		data = spyData.get(2);
		assertEquals(stop2, data.stopFacility);
		assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);

		data = spyData.get(3);
		assertEquals(stop3, data.stopFacility);
		assertTrue(data.returnedDelay > 0);
		lastTime = data.time;
		lastDelay = data.returnedDelay;

		data = spyData.get(4);
		assertEquals(stop3, data.stopFacility);
		assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
		assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);

		data = spyData.get(5);
		assertEquals(stop4, data.stopFacility);
		assertTrue(data.returnedDelay > 0);
		lastTime = data.time;
		lastDelay = data.returnedDelay;

		data = spyData.get(6);
		assertEquals(stop4, data.stopFacility);
		assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
		assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);
	}

	private void setDefaultLinkAttributes(final Link link) {
		link.setLength(1000.0);
		link.setFreespeed(10.0);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1);
	}

	protected static class TestHandleStopSimulation extends TransitQSimulation {
		protected SpyDriver driver = null;
		private final TransitLine line;
		private final TransitRoute route;
		private final Departure departure;

		protected TestHandleStopSimulation(final ScenarioImpl scenario, final EventsManagerImpl events,
				final TransitLine line, final TransitRoute route, final Departure departure) {
			super(scenario, events);
			this.line = line;
			this.route = route;
			this.departure = departure;
		}

		/**
		 * Create one custom Driver to assert conditions
		 */
		@Override
		protected void createAgents() {
			super.createAgents();

			this.driver = new SpyDriver(this.line, this.route, this.departure, this.getAgentTracker(), this);

			BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("transitVehicleType"));
			BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
			capacity.setSeats(Integer.valueOf(101));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicleType.setCapacity(capacity);

			TransitQVehicle veh = new TransitQVehicle(new BasicVehicleImpl(this.driver.getPerson().getId(), vehicleType), 5);
			veh.setDriver(this.driver);
			this.driver.setVehicle(veh);
			this.departure.setVehicleId(veh.getBasicVehicle().getId());
			QLink qlink = this.network.getQueueLink(this.driver.getCurrentLeg().getRoute().getStartLinkId());
			qlink.addParkedVehicle(veh);

			this.scheduleActivityEnd(this.driver, 0);
			Simulation.incLiving();
		}
	}

	protected static class SpyDriver extends TransitDriver {

		public final List<SpyHandleStopData> spyData = new ArrayList<SpyHandleStopData>();

		public SpyDriver(final TransitLine line, final TransitRoute route, final Departure departure,
				final TransitStopAgentTracker agentTracker, final TransitQSimulation sim) {
			super(line, route, departure, agentTracker, sim, new SimpleTransitStopHandler());
		}

		@Override
		public double handleTransitStop(final TransitStopFacility stop, final double now) {
			double delay = super.handleTransitStop(stop, now);
			this.spyData.add(new SpyHandleStopData(stop, now, delay));
			return delay;
		}

	}

	private static class SpyHandleStopData {
		private static final Logger log = Logger.getLogger(TransitQueueSimulationTest.SpyHandleStopData.class);

		public final TransitStopFacility stopFacility;
		public final double time;
		public final double returnedDelay;

		protected SpyHandleStopData(final TransitStopFacility stopFacility, final double time, final double returnedDelay) {
			this.stopFacility = stopFacility;
			this.time = time;
			this.returnedDelay = returnedDelay;
			log.info("handle stop: " + stopFacility.getId() + " " + time + " " + returnedDelay);
		}
	}

	@Test
	public void testStartAndEndTime() {
		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
    config.setQSimConfigGroup(new QSimConfigGroup());

		// build simple network with 2 links
		NetworkImpl network = scenario.getNetwork();
		NodeImpl node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(0.0, 0.0));
		NodeImpl node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000.0, 0.0));
		NodeImpl node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(2000.0, 0.0));
		network.getNodes().put(node1.getId(), node1);
		network.getNodes().put(node2.getId(), node2);
		network.getNodes().put(node3.getId(), node3);
		LinkImpl link1 = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		link1.setFreespeed(10.0);
		link1.setCapacity(2000.0);
		link1.setLength(1000.0);
		LinkImpl link2 = network.getFactory().createLink(scenario.createId("2"), node2.getId(), node3.getId());
		link2.setFreespeed(10.0);
		link2.setCapacity(2000.0);
		link2.setLength(1000.0);
		network.getLinks().put(link1.getId(), link1);
		network.getLinks().put(link2.getId(), link2);
		node2.addInLink(link1);
		node2.addOutLink(link2);

		// build simple schedule with a single line
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		double depTime = 7.0*3600;
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory sb = schedule.getFactory();
		TransitStopFacility stopFacility1 = sb.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(1000, 0), false);
		TransitStopFacility stopFacility2 = sb.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(2000, 0), false);
		schedule.addStopFacility(stopFacility1);
		schedule.addStopFacility(stopFacility2);
		stopFacility1.setLinkId(link1.getId());
		stopFacility2.setLinkId(link2.getId());
		TransitLine tLine = sb.createTransitLine(scenario.createId("1"));
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(link1.getId(), link2.getId(), network);
		TransitRouteStop stop1 = sb.createTransitRouteStop(stopFacility1, Time.UNDEFINED_TIME, 0.0);
		TransitRouteStop stop2 = sb.createTransitRouteStop(stopFacility2, 100.0, 100.0);
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
		stops.add(stop1);
		stops.add(stop2);
		TransitRoute tRoute = sb.createTransitRoute(scenario.createId("1"), route, stops, TransportMode.bus);
		Departure dep = sb.createDeparture(scenario.createId("1"), depTime);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		schedule.addTransitLine(tLine);
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		// prepare test
		EventsManagerImpl events = new EventsManagerImpl();
		FirstLastEventCollector collector = new FirstLastEventCollector();
		events.addHandler(collector);

		// first test without special settings
		TransitQSimulation sim = new TransitQSimulation(scenario, events);
		sim.run();
		assertEquals(depTime, collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
		assertEquals(depTime + 101.0, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
		collector.reset(0);

		// second test with special start/end times
		config.getQSimConfigGroup().setStartTime(depTime + 20.0);
		config.getQSimConfigGroup().setEndTime(depTime + 90.0);
		sim = new TransitQSimulation(scenario, events);
		sim.run();
		assertEquals(depTime + 20.0, collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
		assertEquals(depTime + 90.0, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
	}

	/*package*/ final static class FirstLastEventCollector implements BasicEventHandler {
		public Event firstEvent = null;
		public Event lastEvent = null;

		public void handleEvent(final Event event) {
			if (firstEvent == null) {
				firstEvent = event;
			}
			lastEvent = event;
		}

		public void reset(final int iteration) {
			firstEvent = null;
			lastEvent = null;
		}
	}

	@Test
	public void testEvents() {
		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		// build simple network with 2 links
		NetworkImpl network = scenario.getNetwork();
		NodeImpl node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(0.0, 0.0));
		NodeImpl node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1000.0, 0.0));
		NodeImpl node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(2000.0, 0.0));
		network.getNodes().put(node1.getId(), node1);
		network.getNodes().put(node2.getId(), node2);
		network.getNodes().put(node3.getId(), node3);
		LinkImpl link1 = network.getFactory().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		link1.setFreespeed(10.0);
		link1.setCapacity(2000.0);
		link1.setLength(1000.0);
		LinkImpl link2 = network.getFactory().createLink(scenario.createId("2"), node2.getId(), node3.getId());
		link2.setFreespeed(10.0);
		link2.setCapacity(2000.0);
		link2.setLength(1000.0);
		network.getLinks().put(link1.getId(), link1);
		network.getLinks().put(link2.getId(), link2);
		node2.addInLink(link1);
		node2.addOutLink(link2);

		// build simple schedule with a single line
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		double depTime = 7.0*3600;
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory sb = schedule.getFactory();
		TransitStopFacility stopFacility1 = sb.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(1000, 0), false);
		TransitStopFacility stopFacility2 = sb.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(2000, 0), false);
		schedule.addStopFacility(stopFacility1);
		schedule.addStopFacility(stopFacility2);
		stopFacility1.setLinkId(link1.getId());
		stopFacility2.setLinkId(link2.getId());
		TransitLine tLine = sb.createTransitLine(scenario.createId("1"));
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(link1.getId(), link2.getId(), network);
		TransitRouteStop stop1 = sb.createTransitRouteStop(stopFacility1, Time.UNDEFINED_TIME, 0.0);
		TransitRouteStop stop2 = sb.createTransitRouteStop(stopFacility2, 100.0, 100.0);
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
		stops.add(stop1);
		stops.add(stop2);
		TransitRoute tRoute = sb.createTransitRoute(scenario.createId("1"), route, stops, TransportMode.bus);
		Departure dep = sb.createDeparture(scenario.createId("1"), depTime);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		schedule.addTransitLine(tLine);
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		// build population with 1 person
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		Activity act1 = pb.createActivityFromLinkId("h", link1.getId());
		act1.setEndTime(depTime - 60.0);
		Leg leg1 = pb.createLeg(TransportMode.walk);
		Route route1 = new GenericRouteImpl(link1.getId(), link1.getId());
		route1.setTravelTime(10.0);
		leg1.setRoute(route1);
		Activity act2 = pb.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, link1.getId());
		act2.setEndTime(0.0);
		Leg leg2 = pb.createLeg(TransportMode.pt);
		Route route2 = new ExperimentalTransitRoute(stopFacility1, tLine, tRoute, stopFacility2);
		route2.setTravelTime(100.0);
		leg2.setRoute(route2);
		Activity act3 = pb.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, link1.getId());
		act3.setEndTime(0.0);
		Leg leg3 = pb.createLeg(TransportMode.walk);
		Route route3 = new GenericRouteImpl(link2.getId(), link2.getId());
		route3.setTravelTime(10.0);
		leg3.setRoute(route3);
		Activity act4 = pb.createActivityFromLinkId("w", link2.getId());

		plan.addActivity(act1);
		plan.addLeg(leg1);
		plan.addActivity(act2);
		plan.addLeg(leg2);
		plan.addActivity(act3);
		plan.addLeg(leg3);
		plan.addActivity(act4);
		person.addPlan(plan);
		population.addPerson(person);

		// run sim
		EventsManagerImpl events = new EventsManagerImpl();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		new TransitQSimulation(scenario, events).run();
		List<Event> allEvents = collector.getEvents();

		for (Event event : allEvents) {
			System.out.println(event.toString());
		}

		assertEquals(23, allEvents.size());

		assertTrue(allEvents.get(0) instanceof ActivityEndEventImpl);
		assertEquals("h", ((ActivityEndEventImpl) allEvents.get(0)).getActType());
		assertTrue(allEvents.get(1) instanceof AgentDepartureEventImpl);
		assertTrue(allEvents.get(2) instanceof AgentArrivalEventImpl);
		assertTrue(allEvents.get(3) instanceof ActivityStartEventImpl);
		assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityStartEventImpl) allEvents.get(3)).getActType());
		assertTrue(allEvents.get(4) instanceof ActivityEndEventImpl); // zero activity duration, waiting at stop is considered as leg
		assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityEndEventImpl) allEvents.get(4)).getActType());
		assertTrue(allEvents.get(5) instanceof AgentDepartureEventImpl);

		assertTrue(allEvents.get(6) instanceof AgentDepartureEventImpl); // pt-driver
		assertTrue(allEvents.get(7) instanceof AgentWait2LinkEventImpl); // pt-vehicle

		assertTrue(allEvents.get(8) instanceof VehicleArrivesAtFacilityEvent);
		assertTrue(allEvents.get(9) instanceof PersonEntersVehicleEventImpl);
		assertTrue(allEvents.get(10) instanceof VehicleDepartsAtFacilityEvent);

		assertTrue(allEvents.get(11) instanceof LinkLeaveEventImpl); // pt-vehicle
		assertTrue(allEvents.get(12) instanceof LinkEnterEventImpl); // pt-vehicle

		assertTrue(allEvents.get(13) instanceof VehicleArrivesAtFacilityEvent); // pt-vehicle
		assertTrue(allEvents.get(14) instanceof PersonLeavesVehicleEventImpl);

		assertTrue(allEvents.get(15) instanceof AgentArrivalEventImpl);
		assertTrue(allEvents.get(16) instanceof ActivityStartEventImpl);
		assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityStartEventImpl) allEvents.get(16)).getActType());
		assertTrue(allEvents.get(17) instanceof ActivityEndEventImpl); // zero activity duration, waiting at stop is considered as leg
		assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityEndEventImpl) allEvents.get(17)).getActType());

		assertTrue(allEvents.get(18) instanceof AgentDepartureEventImpl); // walk
		assertTrue(allEvents.get(19) instanceof AgentArrivalEventImpl);
		assertTrue(allEvents.get(20) instanceof ActivityStartEventImpl);
		assertEquals("w", ((ActivityStartEventImpl) allEvents.get(20)).getActType());

		assertTrue(allEvents.get(21) instanceof VehicleDepartsAtFacilityEvent);
		assertTrue(allEvents.get(22) instanceof AgentArrivalEventImpl); // pt-driver
	}
}
