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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.SingletonUmlaufBuilderImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author mrieser
 */
public class TransitDriverTest {

	private static final Logger log = LogManager.getLogger(TransitDriverTest.class);

	public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testInitializationNetworkRoute() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);



		TransitScheduleFactory builder = scenario.getTransitSchedule().getFactory();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));
		ArrayList<Id<Link>> linkIds = new ArrayList<>();

		Network network = (Network) scenario.getNetwork();
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
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, Collections.<TransitRouteStop>emptyList(), "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("T1", VehicleType.class ) );
//		vehType.setCapacity(new VehicleCapacity() );
		vehType.getCapacity().setSeats(5);
		driver.setVehicle(new TransitQVehicle( VehicleUtils.createVehicle(Id.create("V1", Vehicle.class ), vehType ) ) );

		driver.endActivityAndComputeNextState(0.0);
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

	@Test
	void testInitializationDeparture() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);

		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);


		TransitScheduleFactory builder = scenario.getTransitSchedule().getFactory();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, Collections.<TransitRouteStop>emptyList(), "bus");
		double depTime = 9876.5;
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), depTime);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		assertEquals(depTime, driver.getActivityEndTime(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testInitializationStops() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
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

		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);
		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());

		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(5);
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		assertEquals(stop1, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop1, 60), MatsimTestUtils.EPSILON);
		assertEquals(stop2, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop2, 160), MatsimTestUtils.EPSILON);
		assertEquals(stop3, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop3, 260), MatsimTestUtils.EPSILON);
		assertEquals(stop4, driver.getNextTransitStop());
		assertEquals(0, driver.handleTransitStop(stop4, 360), MatsimTestUtils.EPSILON);
		assertEquals(null, driver.getNextTransitStop());
	}

	@Test
	void testHandleStop_EnterPassengers() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
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
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);

		final Config config = ConfigUtils.createConfig() ;
		config.vspExperimental().setGeneratingBoardingDeniedEvent(true);

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);

		final EventHandlerForTesting handler = new EventHandlerForTesting();
		eventsManager.addHandler(handler);
		eventsManager.initProcessing();

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim tqsim = new QSimBuilder(sc.getConfig()) //
				.useDefaults() //
				.build(sc, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);

		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(4));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		PTPassengerAgent agent1 = new FakeAgent(null, stop3);
		PTPassengerAgent agent2 = new FakeAgent(null, stop3);
		PTPassengerAgent agent3 = new FakeAgent(null, stop3);
		PTPassengerAgent agent4 = new FakeAgent(null, stop3);
		PTPassengerAgent agent5 = new FakeAgent(null, stop3);

		tracker.addAgentToStop(10, agent1, stop1.getId());
		tracker.addAgentToStop(15, agent2, stop1.getId());
		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop());
		assertTrue(driver.handleTransitStop(stop1, 50) > 0);
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals(stop1, driver.getNextTransitStop(), "driver must not proceed in stop list when persons entered.");
		assertEquals(0, tracker.getAgentsAtFacility(stop1.getId()).size());
		assertEquals(0.0, driver.handleTransitStop(stop1, 60), MatsimTestUtils.EPSILON, "stop time must be 0 when nobody enters or leaves");
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals(stop2, driver.getNextTransitStop(), "driver must proceed in stop list when no persons entered.");
		assertEquals(stop2, driver.getNextTransitStop(), "driver must return same stop again when queried again without handling stop.");

		tracker.addAgentToStop(100, agent3, stop2.getId());
		double stoptime1 = driver.handleTransitStop(stop2, 150);
		assertTrue(stoptime1 > 0);
		assertEquals(3, queueVehicle.getPassengers().size());
		assertEquals(0, tracker.getAgentsAtFacility(stop2.getId()).size());
		tracker.addAgentToStop(152, agent4, stop2.getId());
		double stoptime2 = driver.handleTransitStop(stop2, 160);
		assertTrue(stoptime2 > 0);
		assertEquals(4, queueVehicle.getPassengers().size());
		assertTrue(stoptime1 > stoptime2,
				"The first stoptime should be larger as it contains door-opening/closing times as well. stoptime1=" + stoptime1 + "  stoptime2=" + stoptime2);
		tracker.addAgentToStop(163, agent5, stop2.getId());
		assertEquals(0.0, driver.handleTransitStop(stop2, 170), MatsimTestUtils.EPSILON, "vehicle should have reached capacity, so not more passenger can enter.");

		eventsManager.finishProcessing();
		assertTrue(handler.isOk);
	}

	private static class EventHandlerForTesting implements EventHandler, BasicEventHandler {
		boolean isOk = false;
		@Override
		public void reset(int iteration) {
		}
		@Override
		public void handleEvent(Event ev) {
			if (ev instanceof BoardingDeniedEvent) {
				BoardingDeniedEvent eve = (BoardingDeniedEvent) ev;
				assertEquals("1", eve.getPersonId().toString());
				assertEquals("1976", eve.getVehicleId().toString());
				isOk = true;
			}
		}
	}

	@Test
	void testHandleStop_ExitPassengers() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
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
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim);
		tqsim.addMobsimEngine(trEngine);


		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
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
		assertEquals(stop1, driver.getNextTransitStop(), "driver must not proceed in stop list when persons entered.");
		assertEquals(2, queueVehicle.getPassengers().size());
		assertEquals(0.0, driver.handleTransitStop(stop1, 60), MatsimTestUtils.EPSILON, "stop time must be 0 when nobody enters or leaves");
		assertEquals(stop2, driver.getNextTransitStop(), "driver must proceed in stop list when no persons entered.");
		assertEquals(stop2, driver.getNextTransitStop(), "driver must return same stop again when queried again without handling stop.");

		assertTrue(driver.handleTransitStop(stop2, 150) > 0);
		assertEquals(0, queueVehicle.getPassengers().size());
		assertEquals(0.0, driver.handleTransitStop(stop2, 160), MatsimTestUtils.EPSILON);
	}

	@Test
	void testHandleStop_CorrectIdentification() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 100, 110));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);

		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		SpyAgent agent = new SpyAgent();
		tracker.addAgentToStop(47, agent, stop1.getId());
		driver.handleTransitStop(stop1, 50);
		assertEquals(tLine, agent.offeredLine);
	}

	@Test
	void testHandleStop_AwaitDepartureTime() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		double departureOffset1 = 60;
		double departureOffset2 = 160;
		TransitRouteStop routeStop1 = builder.createTransitRouteStop(stop1, departureOffset1 - 10.0, departureOffset1);
		routeStop1.setAwaitDepartureTime(true);
		stops.add(routeStop1);
		TransitRouteStop routeStop2 = builder.createTransitRouteStop(stop2, departureOffset2 - 10.0, departureOffset2);
		routeStop2.setAwaitDepartureTime(false);
		stops.add(routeStop2);
		TransitRouteStop routeStop3 = builder.createTransitRouteStopBuilder(stop3).build();
		routeStop3.setAwaitDepartureTime(true);
		stops.add(routeStop3);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(null, null);
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		double departureTime = 9876.0;
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), departureTime);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim);
		tqsim.addMobsimEngine(trEngine);

		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		assertEquals(50.0, driver.handleTransitStop(stop1, departureTime + 10), MatsimTestUtils.EPSILON);
		assertEquals(40.0, driver.handleTransitStop(stop1, departureTime + 20), MatsimTestUtils.EPSILON);
		assertEquals(30.0, driver.handleTransitStop(stop1, departureTime + 30), MatsimTestUtils.EPSILON);
		assertEquals(0.0, driver.handleTransitStop(stop1, departureTime + 60), MatsimTestUtils.EPSILON);

		// stop2 is not awaitDepartureTime
		assertEquals(0.0, driver.handleTransitStop(stop2, departureTime + 110), MatsimTestUtils.EPSILON);

		// stop3 has no departure time
		assertEquals(0.0, driver.handleTransitStop(stop3, departureTime + 210), MatsimTestUtils.EPSILON);
	}

	@Test
	void testExceptionWhenNotEmptyAfterLastStop() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
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
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
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
		assertEquals(0, driver.handleTransitStop(stop2, 160), MatsimTestUtils.EPSILON);
		assertEquals(stop3, driver.getNextTransitStop());
		try {
			assertEquals(0, driver.handleTransitStop(stop3, 260), MatsimTestUtils.EPSILON);
			fail("missing exception: driver still has passengers, although it handled the last stop.");
		}
		catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	void testExceptionWhenNotAllStopsServed() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitLine tLine = builder.createTransitLine(Id.create("L", TransitLine.class));

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 500, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 1500, (double) 0), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 2500, (double) 0), false);
		stop1.setLinkId(Id.create("1", Link.class));
		stop2.setLinkId(Id.create("2", Link.class));
		stop3.setLinkId(Id.create("33", Link.class));
		stops.add(builder.createTransitRouteStop(stop1, 50, 60));
		stops.add(builder.createTransitRouteStop(stop2, 150, 160));
		stops.add(builder.createTransitRouteStop(stop3, 250, 260));
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.create(1, Link.class), Id.create(2, Link.class));
		TransitRoute tRoute = builder.createTransitRoute(Id.create("L1", TransitRoute.class), route, stops, "bus");
		Departure dep = builder.createDeparture(Id.create("L1.1", Departure.class), 9876.0);
		tRoute.addDeparture(dep);
		tLine.addRoute(tRoute);
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(eventsManager);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim tqsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitQSimEngine trEngine = new TransitQSimEngine(tqsim) ;
		tqsim.addMobsimEngine(trEngine);
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(5);
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );

		AbstractTransitDriverAgent driver = new TransitDriverAgentImpl(new SingletonUmlaufBuilderImpl(Collections.singleton(tLine)).build().get(0), TransportMode.car, tracker, trEngine.getInternalInterface());
		TransitQVehicle queueVehicle = new TransitQVehicle(vehicle);
		queueVehicle.setStopHandler(new SimpleTransitStopHandler());
		driver.setVehicle(queueVehicle);

		driver.handleTransitStop(stop1, 60);
		assertEquals(Id.create("2", Link.class), driver.chooseNextLinkId());
		driver.notifyMoveOverNode(Id.create("2", Link.class));
		driver.handleTransitStop(stop2, 120);
		try {
			assertNull(driver.chooseNextLinkId());
			fail("missing exception: driver has not yet handled all stops, although it has no more links in its route.");
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
			throw new RuntimeException("not implemented") ;
		}
	}

}
