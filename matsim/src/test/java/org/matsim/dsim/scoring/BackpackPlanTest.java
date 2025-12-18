package org.matsim.dsim.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegsTest;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BackpackPlanTest {

	@Test
	void testCreatesLeg() {
		Scenario scenario = EventsToLegsTest.createTriangularNetwork();
		var backpackPlan = new BackpackPlan();
		var startLink = Id.createLinkId("l1");
		var endLink = Id.createLinkId("l2");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, Id.create("1", Person.class), startLink, TransportMode.walk, TransportMode.pt));
		backpackPlan.handleEvent(new TeleportationArrivalEvent(30.0, Id.create("1", Person.class), 50.0, TransportMode.walk));
		backpackPlan.handleEvent(new PersonArrivalEvent(30.0, Id.create("1", Person.class), endLink, TransportMode.walk), scenario.getNetwork(), scenario.getTransitSchedule());
		backpackPlan.finish();

		assertSingleLeg(backpackPlan.experiencedPlan(), 10., 20., 50., TransportMode.walk, TransportMode.pt);
		var leg = (Leg) backpackPlan.experiencedPlan().getPlanElements().getFirst();
		var route = leg.getRoute();
		assertEquals(20., route.getTravelTime().seconds(), 1e-9);
		assertEquals(50., route.getDistance(), 1e-9);
		assertEquals(startLink, route.getStartLinkId());
		assertEquals(endLink, route.getEndLinkId());
		assertEquals("generic", route.getRouteType());
	}

	@Test
	void createsLegWithRoute() {
		Scenario scenario = EventsToLegsTest.createTriangularNetwork();
		var backpackPlan = new BackpackPlan();

		Id<Person> agentId = Id.create("1", Person.class);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);
		var startLink = Id.createLinkId("l1");
		var middleLink = Id.createLinkId("l2");
		var endLink = Id.createLinkId("l3");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, agentId, startLink, "car", "car"));
		backpackPlan.handleEvent(new PersonEntersVehicleEvent(10.0, agentId, vehId));
		backpackPlan.handleEvent(new VehicleEntersTrafficEvent(10.0, agentId, startLink, vehId, "car", 1.0));
		backpackPlan.handleEvent(new LinkEnterEvent(11.0, vehId, middleLink));
		backpackPlan.handleEvent(new LinkEnterEvent(16.0, vehId, endLink));
		backpackPlan.handleEvent(new VehicleLeavesTrafficEvent(30.0, agentId, endLink, vehId, "car", 1.0));
		backpackPlan.handleEvent(new PersonArrivalEvent(30.0, agentId, endLink, "car"), scenario.getNetwork(), scenario.getTransitSchedule());
		backpackPlan.finish();

		assertSingleLeg(backpackPlan.experiencedPlan(), 10., 20., 550., "car", "car");
		var leg = (Leg) backpackPlan.experiencedPlan().getPlanElements().getFirst();
		assertEquals(10., leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME));
		assertEquals(vehId, leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME));
		assertInstanceOf(NetworkRoute.class, leg.getRoute());
		var route = (NetworkRoute) leg.getRoute();
		assertEquals(20., route.getTravelTime().seconds(), 1e-9);
		assertEquals(550., route.getDistance(), 1e-9);
		assertEquals(startLink, route.getStartLinkId());
		assertEquals(endLink, route.getEndLinkId());
		assertEquals(1, route.getLinkIds().size());
		assertEquals(middleLink, route.getLinkIds().getFirst());
		assertEquals(vehId, route.getVehicleId());
		assertEquals("links", route.getRouteType());
	}

	@Test
	void testCreatesLegWithRoute_withoutEnteringTraffic() {
		var scenario = EventsToLegsTest.createTriangularNetwork();
		var backpackPlan = new BackpackPlan();

		Id<Vehicle> vehId = Id.createVehicleId("veh1");
		Id<Person> personId = Id.createPersonId("person1");
		var startLinkId = Id.createLinkId("l1");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, personId, startLinkId, "car", "car"));
		backpackPlan.handleEvent(new PersonEntersVehicleEvent(10.0, personId, vehId));
		//driver leaves out vehicle after 10 seconds, no driving at all
		backpackPlan.handleEvent(new PersonArrivalEvent(20.0, personId, startLinkId, "car"), scenario.getNetwork(), scenario.getTransitSchedule());
		backpackPlan.finish();

		assertSingleLeg(backpackPlan.experiencedPlan(), 10., 10., 0., "car", "car");
		var leg = (Leg) backpackPlan.experiencedPlan().getPlanElements().getFirst();
		assertEquals(10., leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME));
		assertEquals(vehId, leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME));
		assertEquals(startLinkId, leg.getRoute().getStartLinkId());
		assertEquals(startLinkId, leg.getRoute().getEndLinkId());
	}

	@Test
	void testCreatesLegWithRoute_withLeavingTrafficOnTheSameLink() {
		var scenario = EventsToLegsTest.createTriangularNetwork();
		var backpackPlan = new BackpackPlan();
		Id<Vehicle> vehId = Id.createVehicleId("veh1");
		Id<Person> personId = Id.createPersonId("person1");
		var startLinkId = Id.createLinkId("l1");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, personId, startLinkId, "car", "car"));
		backpackPlan.handleEvent(new PersonEntersVehicleEvent(10.0, personId, vehId));
		//driver leaves out vehicle after 10 seconds of driving from one end to the other of the initial link
		backpackPlan.handleEvent(
			new VehicleEntersTrafficEvent(10.0, personId, startLinkId, vehId, "car", 0.0));
		backpackPlan.handleEvent(
			new VehicleLeavesTrafficEvent(25.0, personId, startLinkId, vehId, "car", 1.0));
		backpackPlan.handleEvent(new PersonArrivalEvent(20.0, personId, startLinkId, "car"), scenario.getNetwork(), scenario.getTransitSchedule());
		backpackPlan.finish();

		assertSingleLeg(backpackPlan.experiencedPlan(), 10., 10., 500., "car", "car");
		var leg = (Leg) backpackPlan.experiencedPlan().getPlanElements().getFirst();
		assertEquals(10., leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME));
		assertEquals(vehId, leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME));
		assertEquals(startLinkId, leg.getRoute().getStartLinkId());
		assertEquals(startLinkId, leg.getRoute().getEndLinkId());
	}

	@Test
	void testCreatesTransitPassengerRoute() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node node1 = networkFactory.createNode(Id.createNodeId("node1"), new Coord(0.0, 0.0));
		Node node2 = networkFactory.createNode(Id.createNodeId("node2"), new Coord(0.0, 100.0));
		Node node3 = networkFactory.createNode(Id.createNodeId("node3"), new Coord(0.0, 200.0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);

		Id<Link> accessLinkId = Id.createLinkId("accessLink");
		Link accessLink = networkFactory.createLink(accessLinkId, node1, node2);
		network.addLink(accessLink);
		Id<Link> egressLinkId = Id.createLinkId("egressLink");
		Link egressLink = networkFactory.createLink(egressLinkId, node1, node2);
		network.addLink(egressLink);

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory scheduleFactory = schedule.getFactory();

		Id<TransitStopFacility> accessFacilityId = Id.create("accessFacility", TransitStopFacility.class);
		TransitStopFacility accessFacility = scheduleFactory.createTransitStopFacility(accessFacilityId, new Coord(0.0, 0.0), false);
		accessFacility.setLinkId(accessLinkId);
		schedule.addStopFacility(accessFacility);
		Id<TransitStopFacility> egressFacilityId = Id.create("egressFacility", TransitStopFacility.class);
		TransitStopFacility egressFacility = scheduleFactory.createTransitStopFacility(egressFacilityId, new Coord(0.0, 200.0), false);
		egressFacility.setLinkId(egressLinkId);
		schedule.addStopFacility(egressFacility);

		Id<TransitLine> transitLineId = Id.create("testLineId", TransitLine.class);
		TransitLine transitLine = scheduleFactory.createTransitLine(transitLineId);
		schedule.addTransitLine(transitLine);

		Id<TransitRoute> transitRouteId = Id.create("testRouteId", TransitRoute.class);
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(accessLinkId, Collections.emptyList(), egressLinkId);
		TransitRoute transitRoute = scheduleFactory.createTransitRoute(transitRouteId, networkRoute, Collections.emptyList(), "bus");
		transitLine.addRoute(transitRoute);

		Id<Departure> departureId = Id.create("departureId", Departure.class);
		Departure departure = scheduleFactory.createDeparture(departureId, 0.0);
		transitRoute.addDeparture(departure);

		var backpackPlan = new BackpackPlan();

		Id<Vehicle> transitVehiceId = Id.createVehicleId("transitVehicle");

		Id<Person> passengerId = Id.createPersonId("passenger");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, passengerId, accessLinkId, "pt", "pt"));
		backpackPlan.startPtPart(transitLineId, transitRouteId);
		backpackPlan.handleEvent(new PersonEntersVehicleEvent(10.0, passengerId, transitVehiceId));
		backpackPlan.handleEvent(new VehicleDepartsAtFacilityEvent(10., transitVehiceId, accessFacilityId, 0.0));
		backpackPlan.handleEvent(new LinkEnterEvent(10.0, transitVehiceId, egressLinkId));
		backpackPlan.handleEvent(new VehicleArrivesAtFacilityEvent(50, transitVehiceId, egressFacilityId, 0.0));
		backpackPlan.handleEvent(new PersonLeavesVehicleEvent(50.0, passengerId, transitVehiceId));
		backpackPlan.handleEvent(new PersonArrivalEvent(50.0, passengerId, egressLinkId, "pt"), scenario.getNetwork(), scenario.getTransitSchedule());
		backpackPlan.finish();

		// TODO add assertions

//		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getDepartureTime().seconds(), 1e-3);
//		Assertions.assertEquals(1000.0 - 10.0, lh.handledLeg.getLeg().getTravelTime().seconds(), 1e-3);
//		Assertions.assertTrue(lh.handledLeg.getLeg().getRoute() instanceof TransitPassengerRoute);
//
//		TransitPassengerRoute route = (TransitPassengerRoute) lh.handledLeg.getLeg().getRoute();
//		Assertions.assertEquals(100.0, route.getBoardingTime().seconds(), 1e-3);
	}

	private static void assertSingleLeg(Plan experiencedPlan, double departureTime, double travelTime, double distance, String legMode, String routingMode) {
		assertEquals(1, experiencedPlan.getPlanElements().size());

		var el = experiencedPlan.getPlanElements().getFirst();
		assertInstanceOf(Leg.class, el);

		var leg = (Leg) el;
		assertEquals(departureTime, leg.getDepartureTime().seconds(), 1e-9);
		assertEquals(travelTime, leg.getTravelTime().seconds(), 1e-9);
		assertEquals(legMode, leg.getMode());
		assertEquals(routingMode, leg.getRoutingMode());
		assertEquals(distance, leg.getRoute().getDistance(), 1e-9);
	}
}
