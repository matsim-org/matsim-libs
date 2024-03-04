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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoreEventScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mrieser / Simunto
 */
public class CapacityDependentScoringTest {

	@Test
	void testScoring() {
		double normalScore = calcScore(new Fixture(), false);
		double capDepScore = calcScore(new Fixture(), true);

		System.out.println("normal score: " + normalScore);
		System.out.println("capacity dependent score: " + capDepScore);

		// in the normal case, it's a 15min trips at full cost, so it should be -6 * (1/4) = -1.5
		// in the capacity dependent case, the vehicle is empty plus the passenger => occupancy = 0.2, thus the cost should only be 0.8 * original cost => -1.2

		Assertions.assertEquals(-1.5, normalScore, 1e-7);
		Assertions.assertEquals(-1.2, capDepScore, 1e-7);
	}

	private double calcScore(Fixture f, boolean capacityDependent) {
		Config config = f.config;
		Network network = f.scenario.getNetwork();
		ScoringParametersForPerson parameters = new SubpopulationScoringParameters(f.scenario);

		EventsManager events = EventsUtils.createEventsManager();
		RaptorInVehicleCostCalculator inVehicleCostCalculator = capacityDependent ? (new CapacityDependentInVehicleCostCalculator(0.4, 0.3, 0.6, 1.8)) : (new DefaultRaptorInVehicleCostCalculator());
		OccupancyData occData = new OccupancyData();
		OccupancyTracker occTracker = new OccupancyTracker(occData, f.scenario, inVehicleCostCalculator, events, parameters);

		ScoringFunctionFactory testSFF = new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network, config.transit().getTransitModes()));
				scoringFunctionAccumulator.addScoringFunction(new ScoreEventScoring());

				return scoringFunctionAccumulator;
			}
		};

		Population population = f.scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Person person = pf.createPerson(Id.create("1", Person.class));
		population.addPerson(person);

		EventsToScore events2Score = EventsToScore.createWithoutScoreUpdating(f.scenario, testSFF, events);
		events2Score.beginIteration(0, false);
		events.addHandler(occTracker);

		events.addHandler((BasicEventHandler) event -> {
			if (event instanceof PersonScoreEvent) {
				System.out.println(event);
			}
		});

		events.initProcessing();

		Vehicles transitVehicles = f.scenario.getTransitVehicles();
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("bus", VehicleType.class));
		vehType.getCapacity().setSeats(5);
		Vehicle veh = VehicleUtils.createVehicle(Id.create("v1", Vehicle.class), vehType);
		transitVehicles.addVehicleType(vehType);
		transitVehicles.addVehicle(veh);

		Id<Person> driver1 = Id.create("d1", Person.class);
		events.processEvent(new TransitDriverStartsEvent(7*3600 - 100, driver1, veh.getId(), f.fastLineId, f.fastRouteId, Id.create("f1", Departure.class)));
		events.processEvent(new PersonDepartureEvent(7*3600 - 100, driver1, Id.create(1, Link.class), "car", "car"));
		events.processEvent(new PersonEntersVehicleEvent(7*3600 - 100, driver1, veh.getId()));

		events.processEvent(new ActivityEndEvent(7*3600, person.getId(), Id.create(1, Link.class), null, "home", new Coord( 234., 5.67 )));
		events.processEvent(new PersonDepartureEvent(7*3600, person.getId(), Id.create(1, Link.class), "pt", "pt"));
		events.processEvent(new AgentWaitingForPtEvent(7*3600, person.getId(), f.stopAId, f.stopDId));

		events.processEvent(new VehicleArrivesAtFacilityEvent(7*3600-10, veh.getId(), f.stopAId, 0));
		events.processEvent(new PersonEntersVehicleEvent(7*3600-5, person.getId(), veh.getId()));
		events.processEvent(new VehicleDepartsAtFacilityEvent(7*3600, veh.getId(), f.stopAId, 0));

		events.processEvent(new VehicleArrivesAtFacilityEvent(7*3600 + 5*60, veh.getId(), f.stopBId, 0));
		events.processEvent(new VehicleDepartsAtFacilityEvent(7*3600 + 5*60, veh.getId(), f.stopBId, 0));

		events.processEvent(new VehicleArrivesAtFacilityEvent(7*3600 + 10*60, veh.getId(), f.stopCId, 0));
		events.processEvent(new VehicleDepartsAtFacilityEvent(7*3600 + 10*60, veh.getId(), f.stopCId, 0));

		events.processEvent(new VehicleArrivesAtFacilityEvent(7*3600 + 15*60 - 10, veh.getId(), f.stopDId, 0));
		events.processEvent(new PersonLeavesVehicleEvent(7*3600 + 15*60, person.getId(), veh.getId()));
		events.processEvent(new PersonArrivalEvent(7*3600 + 15*60, person.getId(), Id.create(4, Link.class), "pt"));
		events.processEvent(new VehicleDepartsAtFacilityEvent(7*3600 + 16*60, veh.getId(), f.stopDId, 0));

		events.finishProcessing();
		events2Score.finish();
		Double score = events2Score.getAgentScore(person.getId());
		return score;
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

			this.config.transit().setUseTransit(true);
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
