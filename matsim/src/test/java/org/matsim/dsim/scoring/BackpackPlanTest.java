package org.matsim.dsim.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegsTest;
import org.matsim.vehicles.Vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BackpackPlanTest {

	@Test
	void testCreatesLeg() {
		Scenario scenario = EventsToLegsTest.createTriangularNetwork();
		var backpackPlan = new BackpackPlan(scenario.getNetwork());
		var startLink = Id.createLinkId("l1");
		var endLink = Id.createLinkId("l2");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, Id.create("1", Person.class), startLink, TransportMode.walk, TransportMode.pt));
		backpackPlan.handleEvent(new TeleportationArrivalEvent(30.0, Id.create("1", Person.class), 50.0, TransportMode.walk));
		backpackPlan.handleEvent(new PersonArrivalEvent(30.0, Id.create("1", Person.class), endLink, TransportMode.walk));
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
		var backpackPlan = new BackpackPlan(scenario.getNetwork());

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
		backpackPlan.handleEvent(new PersonArrivalEvent(30.0, agentId, endLink, "car"));
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
		var backpackPlan = new BackpackPlan(scenario.getNetwork());

		Id<Vehicle> vehId = Id.createVehicleId("veh1");
		Id<Person> personId = Id.createPersonId("person1");
		var startLinkId = Id.createLinkId("l1");

		backpackPlan.handleEvent(new PersonDepartureEvent(10.0, personId, startLinkId, "car", "car"));
		backpackPlan.handleEvent(new PersonEntersVehicleEvent(10.0, personId, vehId));
		//driver leaves out vehicle after 10 seconds, no driving at all
		backpackPlan.handleEvent(new PersonArrivalEvent(20.0, personId, startLinkId, "car"));
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
		var backpackPlan = new BackpackPlan(scenario.getNetwork());
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
		backpackPlan.handleEvent(new PersonArrivalEvent(20.0, personId, startLinkId, "car"));
		backpackPlan.finish();

		assertSingleLeg(backpackPlan.experiencedPlan(), 10., 10., 500., "car", "car");
		var leg = (Leg) backpackPlan.experiencedPlan().getPlanElements().getFirst();
		assertEquals(10., leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME));
		assertEquals(vehId, leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME));
		assertEquals(startLinkId, leg.getRoute().getStartLinkId());
		assertEquals(startLinkId, leg.getRoute().getEndLinkId());
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
