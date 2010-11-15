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

package org.matsim.pt.qsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.SingletonUmlaufBuilderImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;


/**
 * @author mrieser
 */
public class UmlaufDriverTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(UmlaufDriverTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config c = super.loadConfig(null);
		c.setQSimConfigGroup(new QSimConfigGroup());
	}

	public void testInitializationNetworkRoute() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		ArrayList<Id> linkIds = new ArrayList<Id>();

		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		Node node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(3000, 0));
		Node node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(4000, 0));
		Node node6 = network.createAndAddNode(new IdImpl("6"), new CoordImpl(5000, 0));
		Link link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		Link link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);
		Link link3 = network.createAndAddLink(new IdImpl("3"), node3, node4, 1000.0, 10.0, 3600.0, 1);
		Link link4 = network.createAndAddLink(new IdImpl("4"), node4, node5, 1000.0, 10.0, 3600.0, 1);
		Link link5 = network.createAndAddLink(new IdImpl("5"), node5, node6, 1000.0, 10.0, 3600.0, 1);

		Collections.addAll(linkIds, link2.getId(), link3.getId(), link4.getId());
		NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link5.getId());
		route.setLinkIds(link1.getId(), linkIds, link5.getId());
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, Collections.<TransitRouteStop>emptyList(), "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = null;
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(scenario, new EventsManagerImpl());
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.walk, tracker, tqsim);
		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		driver.setVehicle(queueVehicle);
		driver.endActivityAndAssumeControl(0);
		assertTrue(driver.getCurrentLeg().getRoute() instanceof NetworkRoute);
		NetworkRoute netRoute = (NetworkRoute) driver.getCurrentLeg().getRoute();
		List<Id> expectedLinkIds = route.getLinkIds();
		List<Id> actualLinkIds = netRoute.getLinkIds();
		assertEquals(expectedLinkIds.size(), actualLinkIds.size());
		for (int i = 0, n = expectedLinkIds.size(); i < n; i++) {
			assertEquals(expectedLinkIds.get(i), actualLinkIds.get(i));
		}
		assertEquals(link5.getId(), driver.getDestinationLinkId());
		assertEquals(link2.getId(), driver.chooseNextLinkId());
		driver.notifyMoveOverNode();
		assertEquals(link3.getId(), driver.chooseNextLinkId());
		driver.notifyMoveOverNode();
		assertEquals(link4.getId(), driver.chooseNextLinkId());
		driver.notifyMoveOverNode();
		assertEquals(link5.getId(), driver.chooseNextLinkId());
		driver.notifyMoveOverNode();
		assertEquals(null, driver.chooseNextLinkId());
	}

	private Umlauf buildUmlauf(TransitLine tLine) {
		return new SingletonUmlaufBuilderImpl(Collections.singletonList(tLine)).build().get(0);
	}

	public void testInitializationDeparture() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, Collections.<TransitRouteStop>emptyList(), "bus");
		double depTime = 9876.5;
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), depTime);
		TransitStopAgentTracker tracker = null;
		QSim tqsim = null;
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		assertEquals(depTime, driver.getActivityEndTime(), MatsimTestCase.EPSILON);
	}

	public void testInitializationStops() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("2"), new CoordImpl(1500, 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(new IdImpl("3"), new CoordImpl(2500, 0), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(new IdImpl("4"), new CoordImpl(3500, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));

		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(scenario, new EventsManagerImpl());
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());

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
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
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
		stop1.setLinkId(new IdImpl("dummy"));
		stop2.setLinkId(new IdImpl("dummy"));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());


		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
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
	
	public void testReturnSensiblePlanElements() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(1000, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 100, 110));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.walk, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		assertTrue(driver.getCurrentPlanElement() instanceof Activity);
		driver.endActivityAndAssumeControl(0);
		assertTrue(driver.getCurrentPlanElement() instanceof Leg);
		driver.endLegAndAssumeControl(1);
		assertTrue(driver.getCurrentPlanElement() instanceof Activity);
	}

	public void testHandleStop_CorrectIdentification() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(new IdImpl("L"));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(500, 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl("1"), new CoordImpl(1000, 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 100, 110));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
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
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		double departureTime = 9876.0;
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), departureTime);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
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
		stop1.setLinkId(new IdImpl("dummy"));
		stop2.setLinkId(new IdImpl("dummy"));
		stop3.setLinkId(new IdImpl("dummy"));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl("L1"), route, stops, "bus");
		Departure dep = builder.createDeparture(new IdImpl("L1.1"), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		QSim tqsim = new QSim(sc, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("busType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(new IdImpl(1976), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriver driver = new UmlaufDriver(umlauf, TransportMode.car, tracker, tqsim);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle, 3.0);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
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

		@Override
		public boolean getExitAtStop(final TransitStopFacility stop) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean getEnterTransitRoute(TransitLine line,
				TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
			this.offeredLine = line;
			return false;
		}
	}

}
