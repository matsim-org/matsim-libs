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

package playground.marcel.pt.queuesim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.api.experimental.population.Person;
import org.matsim.core.api.experimental.population.Plan;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.TransitDriverAgent;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

/**
 * @author mrieser
 */
public class TransitQueueSimulationTest extends TestCase {

	/**
	 * Ensure that for each departure an agent is created and departs
	 */
	public void testCreateAgents() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().simulation().setEndTime(8.0*3600);

		// setup: network
		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		NodeImpl node2 = network.createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		NodeImpl node3 = network.createNode(scenario.createId("3"), scenario.createCoord(2000, 0));
		Link link1 = network.createLink(scenario.createId("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		Link link2 = network.createLink(scenario.createId("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleBuilder builder = schedule.getBuilder();

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0));
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0));
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("stop3"), scenario.createCoord(0, 0));
		TransitStopFacility stop4 = builder.createTransitStopFacility(scenario.createId("stop4"), scenario.createCoord(0, 0));
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);
		stop1.setLink(link1);
		stop2.setLink(link1);
		stop3.setLink(link2);
		stop4.setLink(link2);

		NetworkRoute route = new NodeNetworkRoute(link1, link2);
		ArrayList<Link> links = new ArrayList<Link>();
		route.setLinks(link1, links, link2);

		{ // line 1, 1 route, 2 departures
			TransitLine line = builder.createTransitLine(scenario.createId("1"));
			TransitRoute tRoute = builder.createTransitRoute(scenario.createId(">"), route, stops, TransportMode.pt);
			tRoute.addDeparture(builder.createDeparture(scenario.createId("dep1"), 6.0*3600));
			tRoute.addDeparture(builder.createDeparture(scenario.createId("dep2"), 7.0*3600));
			line.addRoute(tRoute);
			schedule.addTransitLine(line);
		}

		{ // line 2, 3 routes, each 1 departure
			TransitLine line = builder.createTransitLine(scenario.createId("2"));
			{ // route 1
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("A"), route, stops, TransportMode.pt);
				tRoute.addDeparture(builder.createDeparture(scenario.createId("dep3"), 8.0*3600));
				line.addRoute(tRoute);
			}
			{ // route 2
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("B"), route, stops, TransportMode.pt);
				tRoute.addDeparture(builder.createDeparture(scenario.createId("dep4"), 8.5*3600));
				line.addRoute(tRoute);
			}
			{ // route 3
				TransitRoute tRoute = builder.createTransitRoute(scenario.createId("C"), route, stops, TransportMode.pt);
				tRoute.addDeparture(builder.createDeparture(scenario.createId("dep5"), 9.0*3600));
				line.addRoute(tRoute);
			}
			schedule.addTransitLine(line);
		}

		scenario.getConfig().simulation().setEndTime(1.0*3600); // prevent running the actual simulation
		TestCreateAgentsSimulation sim = new TestCreateAgentsSimulation(scenario, new Events());
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

	protected static class TestCreateAgentsSimulation extends TransitQueueSimulation {
		public final List<DriverAgent> createdAgents = new ArrayList<DriverAgent>();
		public TestCreateAgentsSimulation(final ScenarioImpl scenario, final Events events) {
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
	public void testAddAgentToStop() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		// setup: network
		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		NodeImpl node2 = network.createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		network.createLink(scenario.createId("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleBuilder builder = schedule.getBuilder();
		TransitLine line = builder.createTransitLine(scenario.createId("1"));

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0));
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);

		// setup: population
		Population population = scenario.getPopulation();
		PopulationBuilder pb = population.getBuilder();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));

		homeAct.setEndTime(7.0*3600 - 10.0);
		// as no transit line runs, make sure to stop the simulation manually.
		scenario.getConfig().simulation().setEndTime(7.0*3600);

		Leg leg = pb.createLeg(TransportMode.pt);
		leg.setRoute(new ExperimentalTransitRoute(stop1, line, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);
		population.addPerson(person);

		// run simulation
		Events events = new Events();
		TransitQueueSimulation simulation = new TransitQueueSimulation(scenario, events);
		simulation.run();

		// check everything
		assertEquals(1, simulation.agentTracker.getAgentsAtStop(stop1).size());
	}

	/**
	 * Tests that a vehicle's handleStop() method is correctly called, e.g.
	 * it is re-called again when returning a delay > 0, and that is is correctly
	 * called when the stop is located on the first link of the network route, on the last
	 * link of the network route, or any intermediary link.
	 */
	public void testHandleStop() {
		// setup: config
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().simulation().setEndTime(8.0*3600);

		// setup: network
		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createNode(scenario.createId("1"), scenario.createCoord(   0, 0));
		NodeImpl node2 = network.createNode(scenario.createId("2"), scenario.createCoord(1000, 0));
		NodeImpl node3 = network.createNode(scenario.createId("3"), scenario.createCoord(2000, 0));
		NodeImpl node4 = network.createNode(scenario.createId("4"), scenario.createCoord(3000, 0));
		NodeImpl node5 = network.createNode(scenario.createId("5"), scenario.createCoord(4000, 0));
		NodeImpl node6 = network.createNode(scenario.createId("6"), scenario.createCoord(5000, 0));
		Link link1 = network.createLink(scenario.createId("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		Link link2 = network.createLink(scenario.createId("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);
		Link link3 = network.createLink(scenario.createId("3"), node3, node4, 1000.0, 10.0, 3600.0, 1);
		Link link4 = network.createLink(scenario.createId("4"), node4, node5, 1000.0, 10.0, 3600.0, 1);
		Link link5 = network.createLink(scenario.createId("5"), node5, node6, 1000.0, 10.0, 3600.0, 1);

		// setup: transit schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleBuilder builder = schedule.getBuilder();
		TransitLine line = builder.createTransitLine(scenario.createId("1"));
		// important: do NOT add the line to the schedule, or agents will be created twice!

		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("stop1"), scenario.createCoord(0, 0));
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("stop2"), scenario.createCoord(0, 0));
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("stop3"), scenario.createCoord(0, 0));
		TransitStopFacility stop4 = builder.createTransitStopFacility(scenario.createId("stop4"), scenario.createCoord(0, 0));
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);

		stop1.setLink(link1); // one stop on the first link of network route, as that one may be specially handled
		stop2.setLink(link3); // some stop in the middle of the network route
		stop3.setLink(link4);
		stop4.setLink(link5); // one stop on the last link of the network route, as that one may be specially handled

		NetworkRoute route = new NodeNetworkRoute(link1, link5);
		ArrayList<Link> links = new ArrayList<Link>();
		Collections.addAll(links, link2, link3, link4);
		route.setLinks(link1, links, link5);

		TransitRoute tRoute = builder.createTransitRoute(scenario.createId(">"), route, stops, TransportMode.pt);
		Departure departure = builder.createDeparture(scenario.createId("dep1"), 6.0*3600);
		tRoute.addDeparture(departure);
		line.addRoute(tRoute);

		// setup: population
		Population population = scenario.getPopulation();
		PopulationBuilder pb = population.getBuilder();
		Person person1 = pb.createPerson(scenario.createId("1"));
		Plan plan1 = pb.createPlan();
		person1.addPlan(plan1);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		homeAct.setEndTime(departure.getDepartureTime() - 60.0);
		Leg leg1 = pb.createLeg(TransportMode.pt);
		leg1.setRoute(new ExperimentalTransitRoute(stop1, line, stop3));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan1.addActivity(homeAct);
		plan1.addLeg(leg1);
		plan1.addActivity(workAct);
		population.addPerson(person1);

		Person person2 = pb.createPerson(scenario.createId("2"));
		Plan plan2 = pb.createPlan();
		person2.addPlan(plan2);
		Leg leg2 = pb.createLeg(TransportMode.pt);
		leg2.setRoute(new ExperimentalTransitRoute(stop3, line, stop4));
		plan2.addActivity(homeAct);
		plan2.addLeg(leg2);
		plan2.addActivity(workAct);
		population.addPerson(person2);


		// run simulation
		Events events = new Events();
		TestSimulation simulation = new TestSimulation(scenario, events, line, tRoute, departure);
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

	protected static class TestSimulation extends TransitQueueSimulation {
		protected SpyDriver driver = null;
		private final TransitLine line;
		private final TransitRoute route;
		private final Departure departure;

		protected TestSimulation(final ScenarioImpl scenario, final Events events,
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

			this.driver = new SpyDriver(this.line, this.route, this.departure, this.agentTracker, this);

			BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("transitVehicleType"));
			BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
			capacity.setSeats(Integer.valueOf(101));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicleType.setCapacity(capacity);

			TransitQueueVehicle veh = new TransitQueueVehicle(new BasicVehicleImpl(this.driver.getPerson().getId(), vehicleType), 5);
			veh.setDriver(this.driver);
			this.driver.setVehicle(veh);
			this.departure.setVehicle(veh.getBasicVehicle());
			QueueLink qlink = this.network.getQueueLink(this.driver.getCurrentLeg().getRoute().getStartLinkId());
			qlink.addParkedVehicle(veh);

			this.scheduleActivityEnd(this.driver);
			Simulation.incLiving();
		}
	}

	protected static class SpyDriver extends TransitDriver {

		public final List<SpyHandleStopData> spyData = new ArrayList<SpyHandleStopData>();

		public SpyDriver(final TransitLine line, final TransitRoute route, final Departure departure,
				final TransitStopAgentTracker agentTracker, final TransitQueueSimulation sim) {
			super(line, route, departure, agentTracker, sim);
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

}
