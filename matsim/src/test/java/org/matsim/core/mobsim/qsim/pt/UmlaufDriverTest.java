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

package org.matsim.core.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.SingletonUmlaufBuilderImpl;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.*;


/**
 * @author mrieser
 */
public class UmlaufDriverTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(UmlaufDriverTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
	}

	public void testInitializationNetworkRoute() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));
		ArrayList<Id<Link>> linkIds = new ArrayList<Id<Link>>();

		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 3000, (double) 0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 4000, (double) 0));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord((double) 5000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		Link link4 = NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node6;
		Link link5 = NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode4, toNode4, 1000.0, 10.0, 3600.0, (double) 1 );

		Collections.addAll(linkIds, link2.getId(), link3.getId(), link4.getId());
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link5.getId());
		route.setLinkIds(link1.getId(), linkIds, link5.getId());
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, Collections.<TransitRouteStop>emptyList(), "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(scenario, eventsManager);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, scenario, eventsManager, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.walk, null, trEngine.getInternalInterface(), scenario, eventsManager);
		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		driver.setVehicle(queueVehicle);
		driver.endActivityAndComputeNextState(0);
		trEngine.getInternalInterface().arrangeNextAgentState(driver) ;
		assertTrue(driver.getCurrentLeg().getRoute() instanceof NetworkRoute);
		NetworkRoute netRoute = (NetworkRoute) driver.getCurrentLeg().getRoute();
		List<Id<Link>> expectedLinkIds = route.getLinkIds();
		List<Id<Link>> actualLinkIds = netRoute.getLinkIds();
		assertEquals(expectedLinkIds.size(), actualLinkIds.size());
		for (int i = 0, n = expectedLinkIds.size(); i < n; i++) {
			assertEquals(expectedLinkIds.get(i), actualLinkIds.get(i));
		}
		assertEquals(link5.getId(), driver.getDestinationLinkId());

		assertEquals(link2.getId(), driver.chooseNextLinkId());
		Id<Link> nextLinkId = driver.chooseNextLinkId() ;
		driver.notifyMoveOverNode(nextLinkId);

		assertEquals(link3.getId(), driver.chooseNextLinkId());
		nextLinkId = driver.chooseNextLinkId() ;
		driver.notifyMoveOverNode(nextLinkId);

		assertEquals(link4.getId(), driver.chooseNextLinkId());
		nextLinkId = driver.chooseNextLinkId() ;
		driver.notifyMoveOverNode(nextLinkId);

		assertEquals(link5.getId(), driver.chooseNextLinkId());
		nextLinkId = driver.chooseNextLinkId() ;
		driver.notifyMoveOverNode(nextLinkId);

		assertEquals(null, driver.chooseNextLinkId());
	}

	private Umlauf buildUmlauf(TransitLine tLine) {
		return new SingletonUmlaufBuilderImpl(Collections.singletonList(tLine)).build().get(0);
	}

	public void testInitializationDeparture() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, Collections.<TransitRouteStop>emptyList(), "bus");
		double depTime = 9876.5;
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), depTime);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		EventsManager events = EventsUtils.createEventsManager();
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(scenario, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, scenario, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), scenario, events);
		assertEquals(depTime, driver.getActivityEndTime(), MatsimTestCase.EPSILON);
	}

	public void testInitializationStops() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 2500, (double) 0), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create("4", TransitStopFacility.class), new Coord((double) 3500, (double) 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		stops.add(builder.createTransitRouteStop(stop4, 350, 360));

		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(scenario, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, scenario, events, mobsimTimer, agentCounter, tqsim.getInternalInterface()) ;
		tqsim.addMobsimEngine(trEngine);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), scenario, events);

		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

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
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);
		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(4));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		PTPassengerAgent agent1 = new FakeAgent(null, stop3);
		PTPassengerAgent agent2 = new FakeAgent(null, stop3);
		PTPassengerAgent agent3 = new FakeAgent(null, stop3);
		PTPassengerAgent agent4 = new FakeAgent(null, stop3);
		PTPassengerAgent agent5 = new FakeAgent(null, stop3);

		tracker.addAgentToStop(10, agent1, stop1.getId());
		tracker.addAgentToStop(20, agent2, stop1.getId());
		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop());
		assertTrue(driver.handleTransitStop(stop1, 50) > 0);
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals("driver must not proceed in stop list when persons entered.",
				stop1, driver.getNextTransitStop());
		assertEquals(0, tracker.getAgentsAtFacility(stop1.getId()).size());
		assertEquals("stop time must be 0 when nobody enters or leaves",
				0.0, driver.handleTransitStop(stop1, 60), MatsimTestCase.EPSILON);
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals("driver must proceed in stop list when no persons entered.",
				stop2, driver.getNextTransitStop());
		assertEquals("driver must return same stop again when queried again without handling stop.",
				stop2, driver.getNextTransitStop());

		tracker.addAgentToStop(110, agent3, stop2.getId());
		double stoptime1 = driver.handleTransitStop(stop2, 150);
		assertTrue(stoptime1 > 0);
		assertEquals(3, queueVehicle.getPassengers().size());
		assertEquals(0, tracker.getAgentsAtFacility(stop2.getId()).size());
		tracker.addAgentToStop(152, agent4, stop2.getId());
		double stoptime2 = driver.handleTransitStop(stop2, 160);
		assertTrue(stoptime2 > 0);
		assertEquals(4, queueVehicle.getPassengers().size());
		assertTrue("The first stoptime should be larger as it contains door-opening/closing times as well. stoptime1=" + stoptime1 + "  stoptime2=" + stoptime2,
				stoptime1 > stoptime2);
		tracker.addAgentToStop(163, agent5, stop2.getId());
		assertEquals("vehicle should have reached capacity, so not more passenger can enter.",
				0.0, driver.handleTransitStop(stop2, 170), MatsimTestCase.EPSILON);
	}

	public void testHandleStop_ExitPassengers() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		stop1.setLinkId(Id.create("dummy", Link.class));
		stop2.setLinkId(Id.create("dummy", Link.class));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine) ;


		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(5);
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		PTPassengerAgent agent1 = new FakeAgent(null, stop1);
		PTPassengerAgent agent2 = new FakeAgent(null, stop1);
		PTPassengerAgent agent3 = new FakeAgent(null, stop2);
		PTPassengerAgent agent4 = new FakeAgent(null, stop2);

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
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 100, 110));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine) ;

		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(5);
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		assertTrue(driver.getCurrentPlanElement() instanceof Activity);
		driver.endActivityAndComputeNextState(0);
		assertTrue(driver.getCurrentPlanElement() instanceof Leg);
		driver.endLegAndComputeNextState(1);
		assertTrue(driver.getCurrentPlanElement() instanceof Activity);
	}

	public void testHandleStop_CorrectIdentification() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 100, 110));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);
		
		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		SpyAgent agent = new SpyAgent();
		tracker.addAgentToStop(47, agent, stop1.getId());
		driver.handleTransitStop(stop1, 50);
		assertEquals(tLine, agent.offeredLine);
	}

	public void testHandleStop_AwaitDepartureTime() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
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
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		double departureTime = 9876.0;
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), departureTime);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);
		
		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
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
		EventsManager events = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 2500, (double) 0), false);
		stop1.setLinkId(Id.create("dummy", Link.class));
		stop2.setLinkId(Id.create("dummy", Link.class));
		stop3.setLinkId(Id.create("dummy", Link.class));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		Config config = ConfigUtils.createConfig();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = QSimUtils.createDefaultQSimWithDefaultTimerAndCounter(sc, events);
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim, config, sc, events, mobsimTimer, agentCounter, tqsim.getInternalInterface());
		tqsim.addMobsimEngine(trEngine);
		
		VehicleType vehType = new VehicleTypeImpl(Id.create("busType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(5);
		vehType.setCapacity(capacity);
		Vehicle vehicle = new VehicleImpl(Id.create(1976, Vehicle.class), vehType);

		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		Umlauf umlauf = buildUmlauf(tLine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(umlauf, TransportMode.car, tracker, trEngine.getInternalInterface(), sc, events);
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		PTPassengerAgent agent1 = new FakeAgent(stop2, stop1);
		tracker.addAgentToStop(55, agent1, stop2.getId());

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

	protected static class SpyAgent implements PTPassengerAgent {
		public TransitLine offeredLine;

		@Override
		public boolean getExitAtStop(final TransitStopFacility stop) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean getEnterTransitRoute(TransitLine line,
				TransitRoute transitRoute, List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
			this.offeredLine = line;
			return false;
		}

		@Override
		public Id<Person> getId() {
			return null;
		}

		@Override
		public double getWeight() {
			return 1.0;
		}

		@Override
		public Id<TransitStopFacility> getDesiredAccessStopId() {
			return null;
		}

		@Override
		public Id<TransitStopFacility> getDesiredDestinationStopId() {
			return null;
		}
		
		@Override
		public void setVehicle(MobsimVehicle veh) {
		}

		@Override
		public MobsimVehicle getVehicle() {
			return null;
		}

		@Override
		public Id<Vehicle> getPlannedVehicleId() {
			return null;
		}

		@Override
		public Id<Link> getCurrentLinkId() {
			return null;
		}

		@Override
		public Id<Link> getDestinationLinkId() {
			return null;
		}

		@Override
		public String getMode() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
	}

}
