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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
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
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests related to the use of custom in-vehicle-cost calculators for SwissRailRaptor.
 *
 * @author mrieser / Simunto GmbH
 */
public class SwissRailRaptorInVehicleCostTest {

	/*fastRoute takes 14min30sec. (==> 870 sec)
	 * slowRoute takes 20min30sec (==> 1230 sec) and departs 1 min later.
	 * slowRoute arrives 7 minutes later ==> 420 sec
	 *
	 * fastRoute must get (870 + 420)/870 = 1.483 times more expensive. --> costFactor at least 1.49
	 * slowRoute must get 420/1230 = 0.3414634146 less expensive --> costFactor at most 0.65
	 * combinations are possible, in which each factor is outside its range to be effective alone, but still be effective when combined.
	 */

	@Test
	void testBaseline_defaultInVehicleCostCalculator_uses_fastRoute() {
		Fixture f = new Fixture();
		runTest(f, new DefaultRaptorInVehicleCostCalculator(), f.fastLineId);
	}

	@Test
	void testBaseline_capacityDependentInVehicleCost_indifferent_uses_fastRoute() {
		Fixture f = new Fixture();
		runTest(f, new CapacityDependentInVehicleCostCalculator(1.0, 0.3, 0.6, 1.8), f.fastLineId);
	}

	@Test
	void test_capacityDependentInVehicleCost_prefersLowOccupancy_uses_slowRoute() {
		Fixture f = new Fixture();

		// from the note above:
		// * fastRoute must get (870 + 420)/870 = 1.483 times more expensive. --> costFactor at least 1.49
		// * slowRoute must get 420/1230 = 0.3414634146 less expensive --> costFactor at most 0.65
		// from below (fillExecutionTracker):
		// * dep at 07:00: 20% occupancy on fast line
		// * dep at 07:10: 80% occupancy on fast line
		// * dep at 07:20: 80% occupancy on fast line
		// Use 1 + 0.49*2 as maximum cost factor, as this applies to 100% occupancy, so it still is 1.49 at 80%.

		runTest(f, new CapacityDependentInVehicleCostCalculator(1.00, 0.2, 0.6, 1.0), f.fastLineId);
		runTest(f, new CapacityDependentInVehicleCostCalculator(0.66, 0.2, 0.6, 1.0), f.fastLineId);
		runTest(f, new CapacityDependentInVehicleCostCalculator(0.65, 0.2, 0.6, 1.0), f.slowLineId);
		runTest(f, new CapacityDependentInVehicleCostCalculator(1.00, 0.2, 0.6, 1.0 + 2*0.48), f.fastLineId);
		runTest(f, new CapacityDependentInVehicleCostCalculator(1.00, 0.2, 0.6, 1.0 + 2*0.49), f.slowLineId);
	}

	@Test
	void test_capacityDependentInVehicleCost_minorHighOccupancyAvoidance_uses_fastRoute() {
		Fixture f = new Fixture();
		runTest(f, new CapacityDependentInVehicleCostCalculator(1.0, 0.3, 0.6, 1.2), f.fastLineId);
	}

	@Test
	void test_capacityDependentInVehicleCost_majorHighOccupancyAvoidance_uses_slowRoute() {
		Fixture f = new Fixture();
		runTest(f, new CapacityDependentInVehicleCostCalculator(1.0, 0.3, 0.6, 2.0), f.slowLineId);
	}

	private void runTest(Fixture f, RaptorInVehicleCostCalculator inVehCostCalcualtor, Id<TransitLine> expectedTransitLine) {
		OccupancyData occupancyData = new OccupancyData();
		OccupancyTracker tracker = new OccupancyTracker(occupancyData, f.scenario, new DefaultRaptorInVehicleCostCalculator(), EventsUtils.createEventsManager(), new SubpopulationScoringParameters(f.scenario));
		fillExecutionTracker(f, tracker);

		SwissRailRaptorData raptorData = SwissRailRaptorData.create(
				f.scenario.getTransitSchedule(), f.scenario.getTransitVehicles(),
				RaptorUtils.createStaticConfig(f.config),
				f.scenario.getNetwork(),
				occupancyData);

		SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, f.config).with(inVehCostCalcualtor).build();

		Facility fromFacility = new FakeFacility(new Coord(900, 900), Id.create("aa", Link.class));
		Facility toFacility = new FakeFacility(new Coord(7100, 1100), Id.create("cd", Link.class));

		List<? extends PlanElement> route1 = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, Time.parseTime("07:00:00"), null));
		Assertions.assertNotNull(route1);

		System.out.println("calculated route:");
		for (PlanElement leg : route1) {
			System.out.println(leg.toString() + "  > " + ((Leg)leg).getRoute().getRouteDescription());
		}

		Assertions.assertEquals(3, route1.size());

		Leg leg1 = (Leg) route1.get(0);
		Assertions.assertEquals("walk", leg1.getMode());

		Leg leg2 = (Leg) route1.get(1);
		Assertions.assertEquals("pt", leg2.getMode());
		TransitPassengerRoute paxRoute1 = (TransitPassengerRoute) leg2.getRoute();
		Assertions.assertEquals(expectedTransitLine, paxRoute1.getLineId());

		Leg leg3 = (Leg) route1.get(2);
		Assertions.assertEquals("walk", leg3.getMode());
	}

	private void fillExecutionTracker(Fixture f, OccupancyTracker tracker) {
		Id<Person> person1 = Id.create(1, Person.class);
		Id<Person> person2 = Id.create(2, Person.class);
		Id<Person> person3 = Id.create(3, Person.class);
		Id<Person> person4 = Id.create(4, Person.class);
		Id<Person> person5 = Id.create(5, Person.class);
		Id<Person> person6 = Id.create(6, Person.class);
		Id<Person> person7 = Id.create(7, Person.class);
		Id<Person> person8 = Id.create(8, Person.class);
		Id<Person> person9 = Id.create(9, Person.class);

		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:51:00"), person1, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:51:00"), person1, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:52:00"), person2, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:52:00"), person2, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:53:00"), person3, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:53:00"), person3, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:54:00"), person4, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:54:00"), person4, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:55:00"), person5, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:55:00"), person5, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:56:00"), person6, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:56:00"), person6, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:57:00"), person7, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:57:00"), person7, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:58:00"), person8, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:58:00"), person8, f.stopAId, f.stopDId));
		tracker.handleEvent(new PersonDepartureEvent(Time.parseTime("06:59:00"), person9, f.stopA.getLinkId(), "pt", "pt"));
		tracker.handleEvent(new AgentWaitingForPtEvent(Time.parseTime("06:59:00"), person9, f.stopAId, f.stopDId));

		Id<Person> driver1 = Id.create(1001, Person.class);
		Id<Person> driver2 = Id.create(1002, Person.class);
		Id<Person> driver3 = Id.create(1003, Person.class);

		Id<Vehicle> vehicle1 = Id.create(1001, Vehicle.class);
		Id<Vehicle> vehicle2 = Id.create(1002, Vehicle.class);
		Id<Vehicle> vehicle3 = Id.create(1003, Vehicle.class);

		Id<Departure> fastDep0 = Id.create("f0", Departure.class);
		Id<Departure> fastDep1 = Id.create("f1", Departure.class);
		Id<Departure> fastDep2 = Id.create("f2", Departure.class);

		// dep at 07:00: person1 --> 1 person in vehicle with capacity 5 --> 20% occupancy
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("06:55:00"), driver1, vehicle1, f.fastLineId, f.fastRouteId, fastDep0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:00:00"), vehicle1, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:00:10"), person1, vehicle1));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:00:30"), vehicle1, f.stopAId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:04:30"), vehicle1, f.stopBId, 0));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:05:00"), vehicle1, f.stopBId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:09:30"), vehicle1, f.stopCId, 0));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:10:00"), vehicle1, f.stopCId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:14:30"), vehicle1, f.stopDId, 0));
		tracker.handleEvent(new PersonLeavesVehicleEvent(Time.parseTime("07:15:00"), person1, vehicle1));

		// dep at 07:10: person2, person3, person4, person5 --> 4 person in vehicle with capacity 5 --> 80% occupancy
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("07:05:00"), driver2, vehicle2, f.fastLineId, f.fastRouteId, fastDep1));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:10:00"), vehicle2, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:05"), person2, vehicle2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:10"), person3, vehicle2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:15"), person4, vehicle2));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:20"), person5, vehicle2));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:10:30"), vehicle2, f.stopAId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:14:30"), vehicle2, f.stopBId, 0));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:15:00"), vehicle2, f.stopBId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:19:30"), vehicle2, f.stopCId, 0));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:20:00"), vehicle2, f.stopCId, 0));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:24:30"), vehicle2, f.stopDId, 0));
		tracker.handleEvent(new PersonLeavesVehicleEvent(Time.parseTime("07:25:00"), person2, vehicle2));
		tracker.handleEvent(new PersonLeavesVehicleEvent(Time.parseTime("07:25:00"), person3, vehicle2));
		tracker.handleEvent(new PersonLeavesVehicleEvent(Time.parseTime("07:25:00"), person4, vehicle2));
		tracker.handleEvent(new PersonLeavesVehicleEvent(Time.parseTime("07:25:00"), person5, vehicle2));

		// dep at 07:20: person6, person7, person8, person9
		tracker.handleEvent(new TransitDriverStartsEvent(Time.parseTime("07:15:00"), driver3, vehicle3, f.fastLineId, f.fastRouteId, fastDep2));
		tracker.handleEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:20:00"), vehicle3, f.stopAId, 0));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:20"), person6, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:20"), person7, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:25"), person8, vehicle3));
		tracker.handleEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:25"), person9, vehicle3));
		tracker.handleEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:20:30"), vehicle3, f.stopAId, 0));
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

			// transit vehicles

			Vehicles transitVehicles = this.scenario.getTransitVehicles();
			VehiclesFactory vf = transitVehicles.getFactory();

			VehicleType vehType = vf.createVehicleType(Id.create("some-bus", VehicleType.class));
			vehType.getCapacity().setSeats(5);
			vehType.getCapacity().setStandingRoom(0);
			transitVehicles.addVehicleType(vehType);

			Vehicle[] fastVehicles = new Vehicle[10];
			Vehicle[] slowVehicles = new Vehicle[10];
			for (int i = 0; i < 10; i++) {
				fastVehicles[i] = vf.createVehicle(Id.create("fast" + i, Vehicle.class), vehType);
				slowVehicles[i] = vf.createVehicle(Id.create("slow" + i, Vehicle.class), vehType);
				transitVehicles.addVehicle(fastVehicles[i]);
				transitVehicles.addVehicle(slowVehicles[i]);
			}

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
					Departure dep = sf.createDeparture(Id.create("f" + i, Departure.class), 7 * 3600 + i * 600);
					dep.setVehicleId(fastVehicles[i].getId());
					fastRoute.addDeparture(dep);
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
					Departure dep = sf.createDeparture(Id.create("s" + i, Departure.class), 7 * 3600 + 60 + i * 600);
					dep.setVehicleId(slowVehicles[i].getId());
					slowRoute.addDeparture(dep);
				}
			}
		}
	}

}
