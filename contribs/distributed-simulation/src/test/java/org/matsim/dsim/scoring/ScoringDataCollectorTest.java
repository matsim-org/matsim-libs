package org.matsim.dsim.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class ScoringDataCollectorTest {

	@Test
	void testTeleportation() {

		var person = Id.createPersonId("p1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");
		var distAggent = mock(DistributedMobsimAgent.class);
		when(distAggent.getId()).thenReturn(person);

		var messaging = mock(SimStepMessaging.class);
		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1001, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2002, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addLink(network.getFactory().createLink(Id.createLinkId(link1), node1, node2));
		network.addLink(network.getFactory().createLink(Id.createLinkId(link2), node2, node3));
		var transitSchedule = mock(TransitSchedule.class);
		var asc = mock(AgentSourcesContainer.class);
		var fbc = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(messaging, network, transitSchedule, asc, fbc);

		collector.registerAgent(distAggent);
		collector.handleEvent(new ActivityEndEvent(1., person, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(1., person, link1, "walk", "walk"));
		collector.handleEvent(new TeleportationArrivalEvent(25, person, 339, "walk"));
		collector.handleEvent(new PersonArrivalEvent(25, person, link2, "walk"));
		collector.handleEvent(new ActivityStartEvent(25, person, link2, null, "work", new Coord(1001, 0)));
		collector.finishPerson(distAggent.getId());

		var backPackCaptor = ArgumentCaptor.forClass(FinishedBackpack.class);
		verify(fbc, times(1)).addBackpack(backPackCaptor.capture());

		var backpack = backPackCaptor.getValue();
		assertEquals(person, backpack.personId());
		var experiencedPlan = backpack.experiencedPlan();
		assertEquals(3, experiencedPlan.getPlanElements().size());

		assertInstanceOf(Activity.class, experiencedPlan.getPlanElements().getFirst());
		var act = (Activity) experiencedPlan.getPlanElements().getFirst();
		assertEquals(link1, act.getLinkId());
		assertEquals("home", act.getType());
		assertEquals(1., act.getEndTime().seconds(), 1e-9);
		assertEquals(OptionalTime.undefined(), act.getStartTime());
		assertEquals(OptionalTime.undefined(), act.getMaximumDuration());

		assertInstanceOf(Leg.class, experiencedPlan.getPlanElements().get(1));
		var leg = (Leg) experiencedPlan.getPlanElements().get(1);
		assertEquals("walk", leg.getMode());
		assertEquals("walk", leg.getRoutingMode());
		assertEquals(24., leg.getTravelTime().seconds(), 1e-9);
		assertEquals(1., leg.getDepartureTime().seconds(), 1e-9);
		assertInstanceOf(GenericRouteImpl.class, leg.getRoute());
		assertEquals(link1, leg.getRoute().getStartLinkId());
		assertEquals(link2, leg.getRoute().getEndLinkId());
		assertEquals(339., leg.getRoute().getDistance(), 1e-9);
		assertEquals(24, leg.getRoute().getTravelTime().seconds());

		assertInstanceOf(Activity.class, experiencedPlan.getPlanElements().getLast());
		var act2 = (Activity) experiencedPlan.getPlanElements().getLast();
		assertEquals(link2, act2.getLinkId());
		assertEquals("work", act2.getType());
		assertEquals(25., act2.getStartTime().seconds(), 1e-9);
		assertEquals(OptionalTime.undefined(), act2.getEndTime());
		assertEquals(OptionalTime.undefined(), act2.getMaximumDuration());
	}

	@Test
	void testTeleportationBetweenPartitions() {
		var person = Id.createPersonId("p1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");
		var distAggent = mock(DistributedMobsimAgent.class);
		when(distAggent.getId()).thenReturn(person);

		var messaging = mock(SimStepMessaging.class);
		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1001, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2002, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addLink(network.getFactory().createLink(Id.createLinkId(link1), node1, node2));
		network.addLink(network.getFactory().createLink(Id.createLinkId(link2), node2, node3));
		var transitSchedule = mock(TransitSchedule.class);
		var asc = mock(AgentSourcesContainer.class);
		var eps = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(messaging, network, transitSchedule, asc, eps);

		collector.registerAgent(distAggent);
		collector.handleEvent(new ActivityEndEvent(1., person, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(1., person, link1, "walk", "walk"));

		// the following simulates how a person leaves and enters a partition. The simulation calls the leave method, which passes a person's backpack
		// to the messaging. Then the other partition passes the received message to its collector, which then receives the events for that agent.
		collector.teleportedPersonLeavesPartition(distAggent);

		// capture the backpack that was passed to messaging.
		var backPackCaptor = ArgumentCaptor.forClass(BackPack.class);
		verify(messaging, times(1)).collectBackPack(backPackCaptor.capture(), anyInt());
		var backpack = backPackCaptor.getValue();

		// create a message from the backpack and pass it back to the collector.
		var msg = SimStepMessage.builder().addBackPack(backpack).build();
		collector.process(msg);

		// now, process the remaining events.
		collector.handleEvent(new TeleportationArrivalEvent(25, person, 339, "walk"));
		collector.handleEvent(new PersonArrivalEvent(25, person, link2, "walk"));
		collector.handleEvent(new ActivityStartEvent(25, person, link2, null, "work", new Coord(1001, 0)));
		collector.finishPerson(distAggent.getId());

		// make sure all plan elements are present. The other values should be the same as in the test above.
		assertEquals(3, backpack.backpackPlan().experiencedPlan().getPlanElements().size());
	}

	@Test
	void testPtTrip() {
		var person = Id.createPersonId("p1");
		var transitVehicle = Id.createVehicleId("bus1");
		var driver = Id.createPersonId("driver1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");

		var distAgent = mock(DistributedMobsimAgent.class);
		when(distAgent.getId()).thenReturn(person);

		var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		var network = scenario.getNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1000, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addLink(network.getFactory().createLink(link1, node1, node2));
		network.addLink(network.getFactory().createLink(link2, node2, node3));

		// Setup Transit Schedule
		var schedule = scenario.getTransitSchedule();
		var stop1 = schedule.getFactory().createTransitStopFacility(Id.create("s1", TransitStopFacility.class), new Coord(0, 0), false);
		stop1.setLinkId(link1);
		var stop2 = schedule.getFactory().createTransitStopFacility(Id.create("s2", TransitStopFacility.class), new Coord(1000, 0), false);
		stop2.setLinkId(link2);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);

		var lineId = Id.create("line1", TransitLine.class);
		var routeId = Id.create("route1", TransitRoute.class);
		var transitLine = schedule.getFactory().createTransitLine(lineId);
		var networkRoute = RouteUtils.createNetworkRoute(List.of(link1, link2));
		var transitRoute = schedule.getFactory().createTransitRoute(routeId, networkRoute, List.of(), "bus");
		transitLine.addRoute(transitRoute);
		schedule.addTransitLine(transitLine);
		var fbc = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(mock(SimStepMessaging.class), network, schedule, mock(AgentSourcesContainer.class), fbc);

		collector.registerAgent(distAgent);

		// 1. Transit Driver starts (this populates transitInformation in the collector)
		collector.handleEvent(new TransitDriverStartsEvent(0.0, driver, transitVehicle, lineId, routeId, Id.create("dep1", Departure.class)));

		// 2. Passenger ends activity and departs
		collector.handleEvent(new ActivityEndEvent(100., person, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(100., person, link1, "pt", "pt"));

		// 3. PT movement and boarding
		collector.handleEvent(new VehicleArrivesAtFacilityEvent(105., transitVehicle, stop1.getId(), 0.0));
		collector.handleEvent(new PersonEntersVehicleEvent(110., person, transitVehicle));
		collector.handleEvent(new VehicleDepartsAtFacilityEvent(115., transitVehicle, stop1.getId(), 0.0));

		collector.handleEvent(new LinkLeaveEvent(120., transitVehicle, link1));
		collector.handleEvent(new LinkEnterEvent(120., transitVehicle, link2));

		collector.handleEvent(new VehicleArrivesAtFacilityEvent(130., transitVehicle, stop2.getId(), 0.0));
		collector.handleEvent(new PersonLeavesVehicleEvent(135., person, transitVehicle));

		// 4. Arrival at destination
		collector.handleEvent(new PersonArrivalEvent(135., person, link2, "pt"));
		collector.handleEvent(new ActivityStartEvent(135., person, link2, null, "work", new Coord(1000, 0)));
		collector.finishPerson(distAgent.getId());

		var backPackCaptor = ArgumentCaptor.forClass(FinishedBackpack.class);
		verify(fbc).addBackpack(backPackCaptor.capture());

		var leg = (Leg) backPackCaptor.getValue().experiencedPlan().getPlanElements().get(1);
		assertEquals("pt", leg.getMode());
		assertInstanceOf(TransitPassengerRoute.class, leg.getRoute());
		assertEquals(35., leg.getTravelTime().seconds(), 1e-9);
		assertEquals(100., leg.getDepartureTime().seconds(), 1e-9);

		var route = (TransitPassengerRoute) leg.getRoute();
		assertEquals(lineId, route.getLineId());
		assertEquals(routeId, route.getRouteId());
		assertEquals(110, route.getBoardingTime().seconds(), 1e-9);
		assertEquals(stop1.getId(), route.getAccessStopId());
		assertEquals(stop2.getId(), route.getEgressStopId());
	}

	@Test
	void testCarTripThreeLinks() {
		var person = Id.createPersonId("p1");
		var vehicle = Id.createVehicleId("p1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");
		var link3 = Id.createLinkId("l3");
		var distAggent = mock(DistributedMobsimAgent.class);
		when(distAggent.getId()).thenReturn(person);

		var messaging = mock(SimStepMessaging.class);
		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1000, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2000, 0));
		var node4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(3000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		// Create 3 links of 1000m each
		for (int i = 1; i <= 3; i++) {
			var link = network.getFactory().createLink(Id.createLinkId("l" + i),
				network.getNodes().get(Id.createNodeId("n" + i)),
				network.getNodes().get(Id.createNodeId("n" + (i + 1))));
			link.setLength(1000);
			network.addLink(link);
		}

		var transitSchedule = mock(TransitSchedule.class);
		var asc = mock(AgentSourcesContainer.class);
		var fbc = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(messaging, network, transitSchedule, asc, fbc);

		collector.registerAgent(distAggent);
		collector.handleEvent(new ActivityEndEvent(100., person, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(100., person, link1, "car", "car"));

		// Movement through 3 links: l1 (start) -> l2 (middle) -> l3 (end)
		collector.handleEvent(new PersonEntersVehicleEvent(100., person, vehicle));
		collector.handleEvent(new VehicleEntersTrafficEvent(101., person, link1, vehicle, "car", 1.0));
		collector.handleEvent(new LinkLeaveEvent(110., vehicle, link1));
		collector.handleEvent(new LinkEnterEvent(110., vehicle, link2));
		collector.handleEvent(new LinkLeaveEvent(120., vehicle, link2));
		collector.handleEvent(new LinkEnterEvent(120., vehicle, link3));
		collector.handleEvent(new VehicleLeavesTrafficEvent(130., person, link3, vehicle, "car", 1.0));
		collector.handleEvent(new PersonLeavesVehicleEvent(130., person, vehicle));

		collector.handleEvent(new PersonArrivalEvent(130., person, link3, "car"));
		collector.handleEvent(new ActivityStartEvent(130., person, link3, null, "work", new Coord(3000, 0)));
		collector.finishPerson(distAggent.getId());

		var backPackCaptor = ArgumentCaptor.forClass(FinishedBackpack.class);
		verify(fbc).addBackpack(backPackCaptor.capture());
		assertEquals(3, backPackCaptor.getValue().experiencedPlan().getPlanElements().size());

		var leg = (Leg) backPackCaptor.getValue().experiencedPlan().getPlanElements().get(1);
		assertEquals("car", leg.getMode());
		assertEquals("car", leg.getRoutingMode());
		assertEquals(30., leg.getTravelTime().seconds(), 1e-9);
		assertEquals(100., leg.getDepartureTime().seconds(), 1e-9);
		var route = (NetworkRoute) leg.getRoute();

		assertEquals(link1, route.getStartLinkId());
		assertEquals(link3, route.getEndLinkId());

		// Check that the middle link (l2) is correctly recorded in the route's link list
		assertEquals(1, route.getLinkIds().size());
		assertEquals(link2, route.getLinkIds().getFirst());
		assertEquals(2000., route.getDistance(), 1e-9);
	}

	@Test
	void testJointTrip() {
		var person1 = Id.createPersonId("p1");
		var person2 = Id.createPersonId("p2");
		var vehicle = Id.createVehicleId("v1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");
		var link3 = Id.createLinkId("l3");

		var distAgent1 = mock(DistributedMobsimAgent.class);
		var distAgent2 = mock(DistributedMobsimAgent.class);
		when(distAgent1.getId()).thenReturn(person1);
		when(distAgent2.getId()).thenReturn(person2);

		var messaging = mock(SimStepMessaging.class);
		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1000, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2000, 0));
		var node4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(3000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		for (int i = 1; i <= 3; i++) {
			var link = network.getFactory().createLink(Id.createLinkId("l" + i),
				network.getNodes().get(Id.createNodeId("n" + i)),
				network.getNodes().get(Id.createNodeId("n" + (i + 1))));
			link.setLength(1000);
			network.addLink(link);
		}

		var transitSchedule = mock(TransitSchedule.class);
		var asc = mock(AgentSourcesContainer.class);
		var fbc = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(messaging, network, transitSchedule, asc, fbc);

		collector.registerAgent(distAgent1);
		collector.registerAgent(distAgent2);

		// Both end activities at l1
		collector.handleEvent(new ActivityEndEvent(100., person1, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(100., person1, link1, "car", "car"));
		collector.handleEvent(new ActivityEndEvent(100., person2, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(100., person2, link1, "ride", "ride"));

		// Both enter the same vehicle
		collector.handleEvent(new PersonEntersVehicleEvent(100., person1, vehicle));
		collector.handleEvent(new PersonEntersVehicleEvent(100., person2, vehicle));

		// Vehicle moves l1 -> l2
		collector.handleEvent(new VehicleEntersTrafficEvent(101., person1, link1, vehicle, "car", 1.0));
		collector.handleEvent(new LinkLeaveEvent(110., vehicle, link1));
		collector.handleEvent(new LinkEnterEvent(110., vehicle, link2));
		collector.handleEvent(new VehicleLeavesTrafficEvent(115., person1, link2, vehicle, "car", 1.0));

		// Person 2 leaves early at link 2
		collector.handleEvent(new PersonLeavesVehicleEvent(115., person2, vehicle));
		collector.handleEvent(new PersonArrivalEvent(115., person2, link2, "ride"));
		collector.handleEvent(new ActivityStartEvent(115., person2, link2, null, "work", new Coord(1000, 0)));

		// Vehicle continues l2 -> l3 for Person 1
		collector.handleEvent(new VehicleEntersTrafficEvent(115., person1, link2, vehicle, "car", 1.0));
		collector.handleEvent(new LinkLeaveEvent(120., vehicle, link2));
		collector.handleEvent(new LinkEnterEvent(120., vehicle, link3));
		collector.handleEvent(new VehicleLeavesTrafficEvent(130., person1, link3, vehicle, "car", 1.0));
		collector.handleEvent(new PersonLeavesVehicleEvent(130., person1, vehicle));
		collector.handleEvent(new PersonArrivalEvent(130., person1, link3, "car"));
		collector.handleEvent(new ActivityStartEvent(130., person1, link3, null, "work", new Coord(2000, 0)));

		collector.finishPerson(distAgent1.getId());
		collector.finishPerson(distAgent2.getId());

		var backPackCaptor = ArgumentCaptor.forClass(FinishedBackpack.class);
		verify(fbc, times(2)).addBackpack(backPackCaptor.capture());

		var backPacks = backPackCaptor.getAllValues();
		var bp1 = backPacks.stream().filter(b -> b.personId().equals(person1)).findFirst().orElseThrow();
		var bp2 = backPacks.stream().filter(b -> b.personId().equals(person2)).findFirst().orElseThrow();

		// Assertions for Person 2 (the rider who left early)
		var leg2 = (Leg) bp2.experiencedPlan().getPlanElements().get(1);
		assertEquals("ride", leg2.getMode());
		assertEquals(15.0, leg2.getTravelTime().seconds());
		assertEquals(link2, leg2.getRoute().getEndLinkId());

		// Assertions for Person 1 (the driver who went the full distance)
		var leg1 = (Leg) bp1.experiencedPlan().getPlanElements().get(1);
		assertEquals("car", leg1.getMode());
		assertEquals(30.0, leg1.getTravelTime().seconds());
		assertEquals(link3, leg1.getRoute().getEndLinkId());
	}

	@Test
	void testAgentStuckDuringTeleportation() {
		var person = Id.createPersonId("p1");
		var link1 = Id.createLinkId("l1");

		var messaging = mock(SimStepMessaging.class);
		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addLink(network.getFactory().createLink(link1, node1, node2));

		var transitSchedule = mock(TransitSchedule.class);
		var asc = mock(AgentSourcesContainer.class);
		var fbc = mock(FinishedBackpackCollector.class);

		var collector = new ScoringDataCollector(messaging, network, transitSchedule, asc, fbc);

		var distAgent = mock(DistributedMobsimAgent.class);
		when(distAgent.getId()).thenReturn(person);
		collector.registerAgent(distAgent);

		collector.handleEvent(new ActivityEndEvent(100., person, link1, null, "home", new Coord(0, 0)));
		collector.handleEvent(new PersonDepartureEvent(110., person, link1, "walk", "walk"));
		collector.handleEvent(new PersonStuckEvent(120., person, link1, "walk"));

		// Verify scoring and plan collection
		var backPackCaptor = ArgumentCaptor.forClass(FinishedBackpack.class);
		verify(fbc).addBackpack(backPackCaptor.capture());

		var experiencedPlan = backPackCaptor.getValue().experiencedPlan();
		assertEquals(2, experiencedPlan.getPlanElements().size());

		assertInstanceOf(Activity.class, experiencedPlan.getPlanElements().get(0));
		assertInstanceOf(Leg.class, experiencedPlan.getPlanElements().get(1));

		var leg = (Leg) experiencedPlan.getPlanElements().get(1);
		assertEquals("walk", leg.getMode());
		assertEquals(110., leg.getDepartureTime().seconds(), 1e-9);
		// Stuck time (120) - departure time (110) = 10
		assertEquals(10., leg.getTravelTime().seconds(), 1e-9);
	}
}
