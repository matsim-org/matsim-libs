/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueNetworkTest.java
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.matsim.api.basic.v01.Id;
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
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimEngine;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.mobsim.queuesim.QueueVehicleImpl;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
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

import playground.marcel.pt.fakes.FakeAgent;

public class TransitQueueNetworkTest extends TestCase {

	/**
	 * Tests that blocking and non-blocking stops are correctly handled on the
	 * first link of a transit vehicle's network route.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testNonBlockingStop_FirstLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(false, 1);

		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(f.normalVehicle, f.qlink2.getAllVehicles().toArray(new QueueVehicle[1])[0]); // first the normal vehicle

		f.simEngine.simStep(102);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(115);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(116); // 100 (departure) + 15 (stop delay) + 1 (buffer2node)
		assertEquals(2, f.qlink2.getAllVehicles().size());
		QueueVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QueueVehicle[2]);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]); // second the transit vehicle

		f.simEngine.simStep(117);
		assertEquals(2, f.qlink2.getAllVehicles().size());
	}

	/**
	 * Tests that blocking stops are correctly handled on the
	 * first link of a transit vehicle's network route.
	 * Note that on the first link, a stop is by definition non-blocking,
	 * as the wait2buffer-queue is seen as similary independent than the transit stop queue!
	 * So, it essentially tests the same thing as {@link #testNonBlockingStop_FirstLink()}.
	 *
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testBlockingStop_FirstLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(true, 1);

		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(f.normalVehicle, f.qlink2.getAllVehicles().toArray(new QueueVehicle[1])[0]); // first the normal vehicle

		f.simEngine.simStep(102);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(115);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(116); // 100 (departure) + 15 (stop delay) + 1 (buffer2node)
		assertEquals(2, f.qlink2.getAllVehicles().size());
		QueueVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QueueVehicle[2]);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]); // second the transit vehicle

		f.simEngine.simStep(117);
		assertEquals(2, f.qlink2.getAllVehicles().size());
	}

	protected static class TestSimEngine extends QueueSimEngine {
		TestSimEngine(final QueueNetwork queueNetwork) {
			super(queueNetwork, new Random(511));
		}
		@Override
		public void simStep(final double time) { // make it public
			super.simStep(time);
		}
		@Override
		public void activateLink(final QueueLink link) { // make it public
			super.activateLink(link);
		}
	}

	protected static class Fixture {
		public final TestSimEngine simEngine;
		public final QueueLink qlink1, qlink2, qlink3;
		public final TransitQueueVehicle transitVehicle;
		public final QueueVehicle normalVehicle;

		public Fixture(final boolean isBlockingStop, final int stopLocation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
			// setup: config
			ScenarioImpl scenario = new ScenarioImpl();
			scenario.getConfig().scenario().setUseTransit(true);

			Id id1 = scenario.createId("1");
			Id id2 = scenario.createId("2");
			Id id3 = scenario.createId("3");
			Id id4 = scenario.createId("4");

			// setup: network
			NetworkLayer network = scenario.getNetwork();
			NodeImpl node1 = network.createNode(id1, scenario.createCoord(   0, 0));
			NodeImpl node2 = network.createNode(id2, scenario.createCoord(1000, 0));
			NodeImpl node3 = network.createNode(id3, scenario.createCoord(2000, 0));
			NodeImpl node4 = network.createNode(id4, scenario.createCoord(3000, 0));
			Link[] links = new Link[4];
			links[1] = network.createLink(id1, node1, node2, 1000.0, 10.0, 3600.0, 1);
			links[2] = network.createLink(id2, node2, node3, 1000.0, 10.0, 3600.0, 1);
			links[3] = network.createLink(id3, node3, node4, 1000.0, 10.0, 3600.0, 1);

			// setup: population
			Population population = scenario.getPopulation();
			PopulationBuilder pb = population.getBuilder();
			Person person = pb.createPerson(id1);
			Plan plan = pb.createPlan();
			person.addPlan(plan);
			Activity act = pb.createActivityFromLinkId("home", id1);
			plan.addActivity(act);
			Leg leg = pb.createLeg(TransportMode.car);
			LinkNetworkRoute route = new LinkNetworkRoute(links[1], links[3]);
			List<Link> links_2 = new ArrayList<Link>();
			links_2.add(links[2]);
			route.setLinks(links[1], links_2, links[3]);
			leg.setRoute(route);
			plan.addLeg(leg);
			plan.addActivity(pb.createActivityFromLinkId("work", id2));
			population.addPerson(person);

			// setup: transit schedule
			TransitSchedule schedule = scenario.getTransitSchedule();
			TransitScheduleBuilder builder = schedule.getBuilder();
			TransitStopFacility stop1 = builder.createTransitStopFacility(id1, scenario.createCoord(0, 0), isBlockingStop);
			schedule.addStopFacility(stop1);
			stop1.setLink(links[stopLocation]);
			TransitLine tLine = builder.createTransitLine(id1);
			NetworkRoute netRoute = new LinkNetworkRoute(links[1], links[3]);
			netRoute.setLinks(links[1], links_2, links[3]);
			ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add(builder.createTransitRouteStop(stop1, 50, 60));
			TransitRoute tRoute = builder.createTransitRoute(id1, netRoute, stops, TransportMode.pt);
			Departure dep = builder.createDeparture(id1, 100);

			// setup: simulation
			QueueNetwork qnet = new QueueNetwork(network);
			this.qlink1 = qnet.getQueueLink(id1);
			this.qlink2 = qnet.getQueueLink(id2);
			this.qlink3 = qnet.getQueueLink(id3);
			this.simEngine = new TestSimEngine(qnet);
			TransitQueueSimulation qsim = new TransitQueueSimulation(scenario, new Events());
			TransitStopAgentTracker tracker = qsim.agentTracker;
			tracker.addAgentToStop(new FakeAgent(null, null), stop1); // just add some agent so the transit vehicle has to stop
			SimulationTimer.setTime(100);

			// need reflection as method is not visible
			// TODO [MR] should not be needed anymore once the test is in the right package
			Method addDepartingVehicle = QueueLink.class.getDeclaredMethod("addDepartingVehicle", QueueVehicle.class);
			addDepartingVehicle.setAccessible(true);

			// setup: vehicles
			BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("testVehicleType"));
			BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
			capacity.setSeats(Integer.valueOf(101));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicleType.setCapacity(capacity);

			this.transitVehicle = new TransitQueueVehicle(new BasicVehicleImpl(id1, vehicleType), 1.0);
			this.qlink1.addParkedVehicle(this.transitVehicle);
			this.transitVehicle.setEarliestLinkExitTime(100);
			TransitDriver tDriver = new TransitDriver(tLine, tRoute, dep, tracker, qsim);
			this.transitVehicle.setDriver(tDriver);
			tDriver.setVehicle(this.transitVehicle);
			addDepartingVehicle.invoke(this.qlink1, this.transitVehicle);

			this.normalVehicle = new QueueVehicleImpl(new BasicVehicleImpl(id2, vehicleType));
			this.qlink1.addParkedVehicle(this.normalVehicle);
			addDepartingVehicle.invoke(this.qlink1, this.normalVehicle);

			PersonAgent nDriver = new PersonAgent((PersonImpl) person, qsim);
			this.normalVehicle.setDriver(nDriver);
			nDriver.setVehicle(this.normalVehicle);
			nDriver.initialize();
			nDriver.activityEnds(99);
		}
	}

}
