/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriverTest.java
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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

import playground.marcel.pt.fakes.FakeAgent;

/**
 * @author mrieser
 */
public class TransitDriverTest extends TestCase {

	private static final Logger log = Logger.getLogger(TransitDriverTest.class);

	public void testInitializationNetworkRoute() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		ArrayList<Link> links = new ArrayList<Link>();

		NetworkLayer network = new NetworkLayer();
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		NodeImpl node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(3000, 0));
		NodeImpl node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(4000, 0));
		NodeImpl node6 = network.createAndAddNode(new IdImpl("6"), new CoordImpl(5000, 0));
		Link link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		Link link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);
		Link link3 = network.createAndAddLink(new IdImpl("3"), node3, node4, 1000.0, 10.0, 3600.0, 1);
		Link link4 = network.createAndAddLink(new IdImpl("4"), node4, node5, 1000.0, 10.0, 3600.0, 1);
		Link link5 = network.createAndAddLink(new IdImpl("5"), node5, node6, 1000.0, 10.0, 3600.0, 1);

		Collections.addAll(links, link2, link3, link4);
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(link1, link5);
		route.setLinks(link1, links, link5);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, Collections.<TransitRouteStop>emptyList(), TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = null;
		TransitQueueSimulation tqsim = null;
		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);

		assertTrue(driver.getCurrentLeg().getRoute() instanceof NetworkRouteWRefs);
		NetworkRouteWRefs netRoute = (NetworkRouteWRefs) driver.getCurrentLeg().getRoute();
		List<Id> expectedLinkIds = route.getLinkIds();
		List<Id> actualLinkIds = netRoute.getLinkIds();
		assertEquals(expectedLinkIds.size(), actualLinkIds.size());
		for (int i = 0, n = expectedLinkIds.size(); i < n; i++) {
			assertEquals(expectedLinkIds.get(i), actualLinkIds.get(i));
		}

		assertEquals(link5, driver.getDestinationLink());
		assertEquals(link2, driver.chooseNextLink());
		driver.moveOverNode();
		assertEquals(link3, driver.chooseNextLink());
		driver.moveOverNode();
		assertEquals(link4, driver.chooseNextLink());
		driver.moveOverNode();
		assertEquals(link5, driver.chooseNextLink());
		driver.moveOverNode();
		assertEquals(null, driver.chooseNextLink());
	}

	public void testInitializationDeparture() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, Collections.<TransitRouteStop>emptyList(), TransportMode.bus);
		double depTime = 9876.5;
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), depTime);
		TransitStopAgentTracker tracker = null;
		TransitQueueSimulation tqsim = null;
		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		assertEquals(depTime, driver.getDepartureTime(), MatsimTestCase.EPSILON);
	}

	public void testInitializationStops() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(1500, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(new IdImpl("3"), new CoordImpl(2500, 0), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(new IdImpl("4"), new CoordImpl(3500, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));

		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = null;
		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		driver.setVehicle(new TransitQueueVehicle(vehicle, 3.0));

		new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl()); // required for Events to be set

		assertEquals(stop1, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop1, 60), MatsimTestCase.EPSILON);
		assertEquals(stop2, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop2, 160), MatsimTestCase.EPSILON);
		assertEquals(stop3, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop3, 260), MatsimTestCase.EPSILON);
		assertEquals(stop4, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop4, 360), MatsimTestCase.EPSILON);
		assertEquals(null, driver.getNextTransitStop());
	}

	public void testHandleStop_EnterPassengers() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(1500, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(new IdImpl("3"), new CoordImpl(1500, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl());

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);

		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		TransitQueueVehicle queueVehicle = new TransitQueueVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);

		PassengerAgent agent1 = new FakeAgent(null, stop3);
		PassengerAgent agent2 = new FakeAgent(null, stop3);
		PassengerAgent agent3 = new FakeAgent(null, stop3);
		PassengerAgent agent4 = new FakeAgent(null, stop3);
		PassengerAgent agent5 = new FakeAgent(null, stop3);

		tracker.addAgentToStop(agent1, stop1);
		tracker.addAgentToStop(agent2, stop1);
		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop());
		assertTrue(driver.handleTransitStop(stop1, 50) > 0);
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals("driver must not proceed in stop list when persons entered.",
				stop1, driver.getNextTransitStop());
		assertEquals(0, tracker.getAgentsAtStop(stop1).size());
		assertEquals("stop time must be 0 when nobody enters or leaves",
				0.0, driver.handleTransitStop(stop1, 60), MatsimTestCase.EPSILON);
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals("driver must proceed in stop list when no persons entered.",
				stop2, driver.getNextTransitStop());
		assertEquals("driver must return same stop again when queried again without handling stop.",
				stop2, driver.getNextTransitStop());

		tracker.addAgentToStop(agent3, stop2);
		double stoptime1 = driver.handleTransitStop(stop2, 150);
		assertTrue(stoptime1 > 0);
		assertEquals(3, queueVehicle.getPassengers().size());
		assertEquals(0, tracker.getAgentsAtStop(stop2).size());
		tracker.addAgentToStop(agent4, stop2);
		double stoptime2 = driver.handleTransitStop(stop2, 160);
		assertTrue(stoptime2 > 0);
		assertEquals(4, queueVehicle.getPassengers().size());
		assertTrue("The first stoptime should be larger as it contains door-opening/closing times as well. stoptime1=" + stoptime1 + "  stoptime2=" + stoptime2,
				stoptime1 > stoptime2);
		tracker.addAgentToStop(agent5, stop2);
		assertEquals("vehicle should have reached capacity, so not more passenger can enter.",
				0.0, driver.handleTransitStop(stop2, 170), MatsimTestCase.EPSILON);
	}

	public void testHandleStop_ExitPassengers() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(1500, 0), false);
		stop1.setLink(new FakeLink(new IdImpl("dummy")));
		stop2.setLink(new FakeLink(new IdImpl("dummy")));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl());

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);

		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		TransitQueueVehicle queueVehicle = new TransitQueueVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);

		PassengerAgent agent1 = new FakeAgent(null, stop1);
		PassengerAgent agent2 = new FakeAgent(null, stop1);
		PassengerAgent agent3 = new FakeAgent(null, stop2);
		PassengerAgent agent4 = new FakeAgent(null, stop2);

		queueVehicle.addPassenger(agent1);
		queueVehicle.addPassenger(agent2);
		queueVehicle.addPassenger(agent3);
		queueVehicle.addPassenger(agent4);

		assertEquals(4, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop());
		assertTrue(driver.handleTransitStop(stop1, 50) > 0);
		assertEquals("driver must not proceed in stop list when persons entered.",
				stop1, driver.getNextTransitStop());
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals("stop time must be 0 when nobody enters or leaves",
				0.0, driver.handleTransitStop(stop1, 60), MatsimTestCase.EPSILON);
		assertEquals("driver must proceed in stop list when no persons entered.",
				stop2, driver.getNextTransitStop());
		assertEquals("driver must return same stop again when queried again without handling stop.",
				stop2, driver.getNextTransitStop());

		assertTrue(driver.handleTransitStop(stop2, 150) > 0);
		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(0.0, driver.handleTransitStop(stop2, 160), MatsimTestCase.EPSILON);
	}

	public void testHandleStop_CorrectIdentification() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl());

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);

		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		TransitQueueVehicle queueVehicle = new TransitQueueVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);

		SpyAgent agent = new SpyAgent();
		tracker.addAgentToStop(agent, stop1);
		driver.handleTransitStop(stop1, 50);
		assertEquals(tLine, agent.offeredLine);
	}

	public void testHandleStop_AwaitDepartureTime() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(500, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(new IdImpl("3"), new CoordImpl(500, 0), false);
		double departureOffset1 = 60;
		double departureOffset2 = 160;
		double departureOffset3 = Time.UNDEFINED_TIME;
		TransitRouteStop routeStop1 = builder.createTransitRouteStop(stop1, departureOffset1 - 10.0, departureOffset1);
		routeStop1.setAwaitDepartureTime(true);
		stops.add(routeStop1);
		TransitRouteStop routeStop2 = builder.createTransitRouteStop(stop2, departureOffset2 - 10.0, departureOffset2);
		routeStop2.setAwaitDepartureTime(false);
		stops.add(routeStop2);
		TransitRouteStop routeStop3 = builder.createTransitRouteStop(stop3, Time.UNDEFINED_TIME, departureOffset3);
		routeStop3.setAwaitDepartureTime(true);
		stops.add(routeStop3);
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		double departureTime = 9876.0;
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), departureTime);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl());

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);

		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		TransitQueueVehicle queueVehicle = new TransitQueueVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);

		assertEquals(50.0, driver.handleTransitStop(stop1, departureTime + 10), MatsimTestCase.EPSILON);
		assertEquals(40.0, driver.handleTransitStop(stop1, departureTime + 20), MatsimTestCase.EPSILON);
		assertEquals(30.0, driver.handleTransitStop(stop1, departureTime + 30), MatsimTestCase.EPSILON);
		assertEquals(0.0, driver.handleTransitStop(stop1, departureTime + 60), MatsimTestCase.EPSILON);

		// stop2 is not awaitDepartureTime
		assertEquals(0.0, driver.handleTransitStop(stop2, departureTime + 110), MatsimTestCase.EPSILON);

		// stop3 has no departure time
		assertEquals(0.0, driver.handleTransitStop(stop3, departureTime + 210), MatsimTestCase.EPSILON);
	}

	public void testExceptionWhenNotEmptyAfterLastStop() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(1500, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(new IdImpl("3"), new CoordImpl(2500, 0), false);
		stop1.setLink(new FakeLink(new IdImpl("dummy")));
		stop2.setLink(new FakeLink(new IdImpl("dummy")));
		stop3.setLink(new FakeLink(new IdImpl("dummy")));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, TransportMode.bus);
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitQueueSimulation tqsim = new TransitQueueSimulation(new ScenarioImpl(), new EventsImpl());

		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);

		TransitDriver driver = new TransitDriver(tLine, tRoute, dep, tracker, tqsim);
		TransitQueueVehicle queueVehicle = new TransitQueueVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);

		PassengerAgent agent1 = new FakeAgent(stop2, stop1);
		tracker.addAgentToStop(agent1, stop2);

		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop());
		driver.handleTransitStop(stop1, 60);
		assertEquals(stop2, driver.getNextTransitStop());
		assertTrue(driver.handleTransitStop(stop2, 160) > 0);
		assertEquals(stop2, driver.getNextTransitStop());
		assertEquals(1, queueVehicle.getPassengers().size());
		assertEquals(0, driver.handleTransitStop(stop2, 160), MatsimTestCase.EPSILON);
		assertEquals(stop3, driver.getNextTransitStop());
		try {
			assertEquals(0, driver.handleTransitStop(stop3, 260), MatsimTestCase.EPSILON);
			fail("missing exception: driver still has passengers, although it handled the last stop.");
		}
		catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
	}

	protected static class SpyAgent implements PassengerAgent {
		public TransitLine offeredLine;

		public boolean getExitAtStop(final TransitStopFacility stop) {
			throw new UnsupportedOperationException();
		}

		public boolean getEnterTransitRoute(final TransitLine tLine, final TransitRoute route) {
			this.offeredLine = tLine;
			return false;
		}
	}

}
