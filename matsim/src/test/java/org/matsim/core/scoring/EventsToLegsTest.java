/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class EventsToLegsTest {

	@Test
	void testCreatesLeg() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), TransportMode.walk, TransportMode.walk));
		eventsToLegs.handleEvent(new TeleportationArrivalEvent(30.0, Id.create("1", Person.class), 50.0, TransportMode.walk));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, Id.create("1", Person.class), Id.create("l2", Link.class), TransportMode.walk));
		assertLeg(lh, 10., 20., 50.0, "walk");
	}

	@Test
	void testCreatesLegWithRoute() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		Id<Person> agentId = Id.create("1", Person.class);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId, Id.createLinkId("l1"), "car", "car"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId, vehId));
		eventsToLegs.handleEvent(new VehicleEntersTrafficEvent(10.0, agentId, Id.createLinkId("l1"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new LinkEnterEvent(11.0, vehId, Id.createLinkId("l2")));
		eventsToLegs.handleEvent(new LinkEnterEvent(16.0, vehId, Id.createLinkId("l3")));
		eventsToLegs.handleEvent(new VehicleLeavesTrafficEvent(30.0, agentId, Id.createLinkId("l3"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, agentId, Id.createLinkId("l3"), "car"));
		assertLeg(lh, 10., 20., 550.0, "car");
		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME), EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME + " missing or incorrect!");
	}

	@Test
	void testCreatesLegWithRoute_jointTrip() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);

		//agent 1 enters vehicle
		Id<Person> agentId1 = Id.create("1", Person.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId1, Id.createLinkId("l1"), "car", "car"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId1, vehId));

		//agent 2 enters vehicle
		Id<Person> agentId2 = Id.create("2", Person.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId2, Id.createLinkId("l1"), "ride", "ride"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId2, vehId));

		//vehicle drives from l1 to l2
		eventsToLegs.handleEvent(
				new VehicleEntersTrafficEvent(10.0, agentId1, Id.createLinkId("l1"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new LinkEnterEvent(11.0, vehId, Id.createLinkId("l2")));
		eventsToLegs.handleEvent(
				new VehicleLeavesTrafficEvent(15.0, agentId1, Id.createLinkId("l2"), vehId, "car", 1.0));

		//agent 2 leaves vehicle
		eventsToLegs.handleEvent(new PersonArrivalEvent(15.0, agentId2, Id.createLinkId("l2"), "ride"));
		assertLeg(lh, 10., 5., 500.0, "ride");

		//vehicle drives from l2 to l3
		eventsToLegs.handleEvent(
				new VehicleEntersTrafficEvent(15.0, agentId1, Id.createLinkId("l2"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new LinkEnterEvent(16.0, vehId, Id.createLinkId("l3")));
		eventsToLegs.handleEvent(
				new VehicleLeavesTrafficEvent(30.0, agentId1, Id.createLinkId("l3"), vehId, "car", 1.0));

		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, agentId1, Id.createLinkId("l3"), "car"));
		assertLeg(lh, 10., 20., 550.0, "car");
		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME), EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME + " missing or incorrect!");
	}

	@Test
	void testCreatesLegWithRoute_withoutEnteringTraffic() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);

		Id<Person> agentId1 = Id.create("1", Person.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId1, Id.createLinkId("l1"), "car", "car"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId1, vehId));
		//driver leaves out vehicle after 10 seconds, no driving at all
		eventsToLegs.handleEvent(new PersonArrivalEvent(20.0, agentId1, Id.createLinkId("l1"), "car"));
		assertLeg(lh, 10., 10., 0.0, "car");
		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME), EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME + " missing or incorrect!");
	}

	@Test
	void testCreatesLegWithRoute_withLeavingTrafficOnTheSameLink() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);

		Id<Person> agentId1 = Id.create("1", Person.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId1, Id.createLinkId("l1"), "car", "car"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId1, vehId));
		//driver leaves out vehicle after 10 seconds of driving from one end to the other of the initial link
		eventsToLegs.handleEvent(
				new VehicleEntersTrafficEvent(10.0, agentId1, Id.createLinkId("l1"), vehId, "car", 0.0));
		eventsToLegs.handleEvent(
				new VehicleLeavesTrafficEvent(25.0, agentId1, Id.createLinkId("l1"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new PersonArrivalEvent(20.0, agentId1, Id.createLinkId("l1"), "car"));
		assertLeg(lh, 10., 10., 500.0, "car");
		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME), EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME + " missing or incorrect!");
	}

	@Test
	void testCreatesTransitPassengerRoute() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		
		Node node1 = networkFactory.createNode(Id.createNodeId("node1"), new Coord(0.0, 0.0));
		Node node2 = networkFactory.createNode(Id.createNodeId("node2"), new Coord(0.0, 0.0));
		network.addNode(node1);
		network.addNode(node2);
		
		Id<Link> accessLinkId = Id.createLinkId("accessLink");
		Link accessLink = networkFactory.createLink(accessLinkId, node1, node2);
		network.addLink(accessLink);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		
		Id<TransitStopFacility> accessFacilityId = Id.create("accessFacility", TransitStopFacility.class);
		TransitStopFacility accessFacility = scheduleFactory.createTransitStopFacility(accessFacilityId, new Coord(0.0, 0.0), false);
		accessFacility.setLinkId(accessLinkId);
		schedule.addStopFacility(accessFacility);
		
		Id<TransitLine> transitLineId = Id.create("testLineId", TransitLine.class);
		TransitLine transitLine = scheduleFactory.createTransitLine(transitLineId);
		schedule.addTransitLine(transitLine);
		
		Id<TransitRoute> transitRouteId = Id.create("testRouteId", TransitRoute.class);
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(accessLinkId, Collections.emptyList(), accessLinkId);
		TransitRoute transitRoute = scheduleFactory.createTransitRoute(transitRouteId, networkRoute, Collections.emptyList(), "bus");
		transitLine.addRoute(transitRoute);
		
		Id<Departure> departureId = Id.create("departureId", Departure.class);
		Departure departure = scheduleFactory.createDeparture(departureId, 0.0);
		transitRoute.addDeparture(departure);
		
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		
		Id<Person> transitDriverId = Id.createPersonId("transitDriver");
		Id<Vehicle> transitVehiceId = Id.createVehicleId("transitVehicle");
		
		Id<Person> passengerId = Id.createPersonId("passenger");
		
		eventsToLegs.handleEvent(new TransitDriverStartsEvent(0.0, transitDriverId, transitVehiceId, transitLineId, transitRouteId, departureId));
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, passengerId, accessLinkId, "pt", "pt"));
		eventsToLegs.handleEvent(new VehicleArrivesAtFacilityEvent(50.0, transitVehiceId, accessFacilityId, 0.0));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(100.0, passengerId, transitVehiceId));
		eventsToLegs.handleEvent(new PersonArrivalEvent(1000.0, passengerId, accessLinkId, "pt"));
		
		Assertions.assertEquals(10.0, lh.handledLeg.getLeg().getDepartureTime().seconds(), 1e-3);
		Assertions.assertEquals(1000.0 - 10.0, lh.handledLeg.getLeg().getTravelTime().seconds(), 1e-3);
		Assertions.assertTrue(lh.handledLeg.getLeg().getRoute() instanceof TransitPassengerRoute);
		
		TransitPassengerRoute route = (TransitPassengerRoute) lh.handledLeg.getLeg().getRoute();
		Assertions.assertEquals(100.0, route.getBoardingTime().seconds(), 1e-3);
	}

	private static Scenario createTriangularNetwork() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Network network = scenario.getNetwork();

		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("n1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("n2", Node.class), new Coord((double) 50, (double) 100));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("n3", Node.class), new Coord((double) 50, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("n4", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;

		// add links
		NetworkUtils.createAndAddLink(network,Id.create("l1", Link.class), fromNode, toNode, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("l2", Link.class), fromNode1, toNode1, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("l3", Link.class), fromNode2, toNode2, 50.0, 0.1, 3600.0, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("l4", Link.class), fromNode3, toNode3, 50.0, 0.1, 3600.0, (double) 1 );

		return scenario;
	}

	private void assertLeg(RememberingLegHandler lh, double departureTime, double travelTime, double distance,
			String mode) {
		Assertions.assertNotNull(lh.handledLeg);
		Assertions.assertEquals(departureTime, lh.handledLeg.getLeg().getDepartureTime().seconds(), 1e-9);
		Assertions.assertEquals(travelTime, lh.handledLeg.getLeg().getTravelTime().seconds(), 1e-9);
		Assertions.assertEquals(travelTime, lh.handledLeg.getLeg().getRoute().getTravelTime().seconds(), 1e-9);
		Assertions.assertEquals(distance, lh.handledLeg.getLeg().getRoute().getDistance(), 1e-9);
		Assertions.assertEquals(mode, lh.handledLeg.getLeg().getMode());
	}

	private static class RememberingLegHandler implements LegHandler {

		/*package*/ PersonExperiencedLeg handledLeg = null;

		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			this.handledLeg = leg;
		}
	}

}
