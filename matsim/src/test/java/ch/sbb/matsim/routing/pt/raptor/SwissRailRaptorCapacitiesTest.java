/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests related to the capacity-constraint routing of SwissRailRaptor.
 *
 * @author mrieser / Simunto GmbH
 */
public class SwissRailRaptorCapacitiesTest {

	@Test
	void testUseSlowerAlternative() {
		Fixture f = new Fixture();

		// default case

		OccupancyData occupancyData = new OccupancyData();

		SwissRailRaptorData raptorData = SwissRailRaptorData.create(
				f.scenario.getTransitSchedule(), null,
				RaptorUtils.createStaticConfig(f.config),
				f.scenario.getNetwork(),
				occupancyData);

		SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, f.config).build();

		Facility fromFacility = new FakeFacility(new Coord(900, 900), Id.create("aa", Link.class));
		Facility toFacility = new FakeFacility(new Coord(7100, 1100), Id.create("cd", Link.class));

		List<? extends PlanElement> route1 = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, Time.parseTime("07:00:00"), null));
		Assertions.assertNotNull(route1);
		System.out.println("uncongested route:");
		for (PlanElement leg : route1) {
			System.out.println(leg.toString() + "  > " + ((Leg)leg).getRoute().getRouteDescription());
		}
		Assertions.assertEquals(3, route1.size());

		Leg leg1 = (Leg) route1.get(0);
		Assertions.assertEquals("walk", leg1.getMode());

		Leg leg2 = (Leg) route1.get(1);
		Assertions.assertEquals("pt", leg2.getMode());
		TransitPassengerRoute paxRoute1 = (TransitPassengerRoute) leg2.getRoute();
		Assertions.assertEquals(f.fastLineId, paxRoute1.getLineId());

		Leg leg3 = (Leg) route1.get(2);
		Assertions.assertEquals("walk", leg3.getMode());

		// with delays at entering

		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class);
		srrConfig.setUseCapacityConstraints(true);
		occupancyData = new OccupancyData();
		OccupancyTracker tracker = new OccupancyTracker(occupancyData, f.scenario, new DefaultRaptorInVehicleCostCalculator(), EventsUtils.createEventsManager(), new SubpopulationScoringParameters(f.scenario));

		raptorData = SwissRailRaptorData.create(
				f.scenario.getTransitSchedule(), null,
				RaptorUtils.createStaticConfig(f.config),
				f.scenario.getNetwork(),
				occupancyData);

		raptor = new SwissRailRaptor.Builder(raptorData, f.config).build();

		Id<Person> person1 = Id.create(1, Person.class);
		Id<Person> person2 = Id.create(2, Person.class);
		Id<Person> person3 = Id.create(3, Person.class);
		Id<Person> person4 = Id.create(4, Person.class);
		Id<Person> person5 = Id.create(5, Person.class);
		Id<Person> person6 = Id.create(6, Person.class);
		Id<Person> person7 = Id.create(7, Person.class);
		Id<Person> person8 = Id.create(8, Person.class);
		Id<Person> person9 = Id.create(9, Person.class);

		Id<Link> linkId = Id.create(1, Link.class);
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:51:00"), person1, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:51:00"), person1, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:52:00"), person2, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:52:00"), person2, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:53:00"), person3, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:53:00"), person3, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:54:00"), person4, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:54:00"), person4, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:55:00"), person5, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:55:00"), person5, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:56:00"), person6, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:56:00"), person6, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:57:00"), person7, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:57:00"), person7, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:58:00"), person8, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:58:00"), person8, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:59:00"), person9, linkId, "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:59:00"), person9, f.stopAId, f.stopDId));

		Id<Person> driver1 = Id.create(1001, Person.class);
		Id<Person> driver2 = Id.create(1002, Person.class);
		Id<Person> driver3 = Id.create(1003, Person.class);

		Id<Vehicle> vehicle1 = Id.create(1001, Vehicle.class);
		Id<Vehicle> vehicle2 = Id.create(1002, Vehicle.class);
		Id<Vehicle> vehicle3 = Id.create(1003, Vehicle.class);

		Id<Departure> fastDep1 = Id.create("f1", Departure.class);
		Id<Departure> fastDep2 = Id.create("f2", Departure.class);
		Id<Departure> fastDep3 = Id.create("f3", Departure.class);

		// dep at 07:00: person1, person2
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("06:55:00"), driver1, vehicle1, f.fastLineId, f.fastRouteId, fastDep1));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("06:55:00"), driver1, vehicle1));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:00:00"), vehicle1, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:00:10"), person1, vehicle1));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:00:20"), person2, vehicle1));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:00:30"), vehicle1, f.stopAId, 0));

		// dep at 07:10: person3, person4, person5
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("07:05:00"), driver2, vehicle2, f.fastLineId, f.fastRouteId, fastDep2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:05:00"), driver2, vehicle2));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:10:00"), vehicle2, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:10"), person3, vehicle2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:20"), person4, vehicle2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:25"), person5, vehicle2));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:10:30"), vehicle2, f.stopAId, 0));

		// dep at 07:20: person6, person7, person8, person9
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("07:15:00"), driver3, vehicle3, f.fastLineId, f.fastRouteId, fastDep3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:15:00"), driver3, vehicle3));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:20:00"), vehicle2, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:10"), person6, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:20"), person7, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:25"), person8, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:25"), person9, vehicle3));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:20:30"), vehicle3, f.stopAId, 0));

		List<? extends PlanElement> route2 = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, Time.parseTime("07:00:00"), null));
		Assertions.assertNotNull(route2);
		System.out.println("congested route:");
		for (PlanElement leg : route2) {
			System.out.println(leg.toString() + "  > " + ((Leg)leg).getRoute().getRouteDescription());
		}
		Assertions.assertEquals(3, route2.size());

		leg1 = (Leg) route2.get(0);
		Assertions.assertEquals("walk", leg1.getMode());

		leg2 = (Leg) route2.get(1);
		Assertions.assertEquals("pt", leg2.getMode());
		TransitPassengerRoute paxRoute2 = (TransitPassengerRoute) leg2.getRoute();
		Assertions.assertEquals(f.slowLineId, paxRoute2.getLineId());

		leg3 = (Leg) route2.get(2);
		Assertions.assertEquals("walk", leg3.getMode());
	}

	private static class Fixture {
		/* Main idea of scenario: two parallel lines, one a bit slower than the other.
		   Normally, agents should prefer the faster line, but if that one is over capacity, then
		   agents should start switching to the slower line.
		 */

		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(this.config);

		final Id<TransitStopFacility> stopAId = Id.create("A", TransitStopFacility.class);
		final Id<TransitStopFacility> stopBId = Id.create("B", TransitStopFacility.class);
		final Id<TransitStopFacility> stopCId = Id.create("C", TransitStopFacility.class);
		final Id<TransitStopFacility> stopDId = Id.create("D", TransitStopFacility.class);

		final TransitStopFacility stopA;
		final TransitStopFacility stopB;
		final TransitStopFacility stopC;
		final TransitStopFacility stopD;

		final Id<TransitLine> fastLineId = Id.create("fast", TransitLine.class);
		final Id<TransitLine> slowLineId = Id.create("slow", TransitLine.class);

		final Id<TransitRoute> fastRouteId = Id.create("fast", TransitRoute.class);
		final Id<TransitRoute> slowRouteId = Id.create("slow", TransitRoute.class);

		public Fixture() {
			// network

			Network network = this.scenario.getNetwork();
			NetworkFactory nf = network.getFactory();

			Node nodeA = nf.createNode(Id.create("a", Node.class), new Coord(1000, 1000));
			Node nodeB = nf.createNode(Id.create("b", Node.class), new Coord(3000, 1000));
			Node nodeC = nf.createNode(Id.create("c", Node.class), new Coord(5000, 1000));
			Node nodeD = nf.createNode(Id.create("d", Node.class), new Coord(7000, 1000));

			network.addNode(nodeA);
			network.addNode(nodeB);
			network.addNode(nodeC);
			network.addNode(nodeD);

			Link linkAA = NetworkUtils.createLink(Id.create("aa", Link.class), nodeA, nodeA, network, 3000, 20, 2000, 1);
			Link linkAB = NetworkUtils.createLink(Id.create("ab", Link.class), nodeA, nodeB, network, 3000, 20, 2000, 1);
			Link linkBC = NetworkUtils.createLink(Id.create("bc", Link.class), nodeB, nodeC, network, 3000, 20, 2000, 1);
			Link linkCD = NetworkUtils.createLink(Id.create("cd", Link.class), nodeC, nodeD, network, 3000, 20, 2000, 1);

			network.addLink(linkAA);
			network.addLink(linkAB);
			network.addLink(linkBC);
			network.addLink(linkCD);

			// transit schedule

			TransitSchedule schedule = this.scenario.getTransitSchedule();
			TransitScheduleFactory sf = schedule.getFactory();

			this.stopA = sf.createTransitStopFacility(this.stopAId, new Coord(1000, 1000), false);
			this.stopB = sf.createTransitStopFacility(this.stopBId, new Coord(3000, 1000), false);
			this.stopC = sf.createTransitStopFacility(this.stopCId, new Coord(5000, 1000), false);
			this.stopD = sf.createTransitStopFacility(this.stopDId, new Coord(7000, 1000), false);

			this.stopA.setLinkId(linkAA.getId());
			this.stopB.setLinkId(linkAB.getId());
			this.stopC.setLinkId(linkBC.getId());
			this.stopD.setLinkId(linkCD.getId());

			schedule.addStopFacility(this.stopA);
			schedule.addStopFacility(this.stopB);
			schedule.addStopFacility(this.stopC);
			schedule.addStopFacility(this.stopD);

			{ // fast line
				TransitLine fastLine = sf.createTransitLine(this.fastLineId);
				List<TransitRouteStop> fastStops = new ArrayList<>();
				fastStops.add(sf.createTransitRouteStopBuilder(this.stopA).departureOffset(0.0).build());
				fastStops.add(sf.createTransitRouteStopBuilder(this.stopB).arrivalOffset(5 * 60 - 30).departureOffset(5 * 60).build());
				fastStops.add(sf.createTransitRouteStopBuilder(this.stopC).arrivalOffset(10 * 60 - 30).departureOffset(10 * 60).build());
				fastStops.add(sf.createTransitRouteStopBuilder(this.stopD).arrivalOffset(15 * 60 - 30).build());

				NetworkRoute route = RouteUtils.createNetworkRoute(List.of(linkAA.getId(), linkAB.getId(), linkBC.getId(), linkCD.getId()), network);

				TransitRoute fastRoute = sf.createTransitRoute(this.fastRouteId, route, fastStops, "bus");
				fastLine.addRoute(fastRoute);
				schedule.addTransitLine(fastLine);

				for (int i = 0; i < 10; i++) {
					fastRoute.addDeparture(sf.createDeparture(Id.create("f" + i, Departure.class), 7 * 3600 + i * 600));
				}
			}

			{ // slow line
				TransitLine slowLine = sf.createTransitLine(this.slowLineId);
				List<TransitRouteStop> slowStops = new ArrayList<>();
				slowStops.add(sf.createTransitRouteStopBuilder(this.stopA).departureOffset(0.0).build());
				slowStops.add(sf.createTransitRouteStopBuilder(this.stopB).arrivalOffset(7 * 60 - 30).departureOffset(7 * 60).build());
				slowStops.add(sf.createTransitRouteStopBuilder(this.stopC).arrivalOffset(14 * 60 - 30).departureOffset(14 * 60).build());
				slowStops.add(sf.createTransitRouteStopBuilder(this.stopD).arrivalOffset(21 * 60 - 30).build());

				NetworkRoute route = RouteUtils.createNetworkRoute(List.of(linkAA.getId(), linkAB.getId(), linkBC.getId(), linkCD.getId()), network);

				TransitRoute slowRoute = sf.createTransitRoute(this.slowRouteId, route, slowStops, "bus");
				slowLine.addRoute(slowRoute);
				schedule.addTransitLine(slowLine);

				for (int i = 0; i < 10; i++) {
					slowRoute.addDeparture(sf.createDeparture(Id.create("s" + i, Departure.class), 7 * 3600 + 60 + i * 600));
				}
			}
		}
	}

}
