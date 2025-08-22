/* *********************************************************************** *
 * project: org.matsim.*
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

package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.events.RailsimDetourEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import org.opentest4j.AssertionFailedError;

public class RailsimIntegrationTest extends AbstractIntegrationTest {

	@Test
	void testMicroSimpleBiDirectionalTrack() {
		// We have 6 trains going sequentially from t1 to t3.
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microSimpleBiDirectionalTrack"));

		// trains depart in 10min intervals
		assertThat(result).allTrainsArrived()
			.trainHasLastArrival("train1", 29180.0)
			.trainHasLastArrival("train2", 29180.0 + 1 * 600)
			.trainHasLastArrival("train3", 29180.0 + 2 * 600)
			.trainHasLastArrival("train4", 29180.0 + 3 * 600)
			.trainHasLastArrival("train5", 29180.0 + 4 * 600)
			.trainHasLastArrival("train6", 29180.0 + 5 * 600);

		// check that there is only one train on each link at a time
		for (String link : result.getScenario().getNetwork().getLinks().keySet().stream().map(Id::toString).toList()) {
			assertSingleTrainOnLink(result.getEvents(), link);
		}
	}

	@Test
	void testMesoUniDirectionalVaryingCapacities() {
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoUniDirectionalVaryingCapacities"));

		// print events of train1 for debugging
		List<RailsimTrainStateEvent> train1events = filterTrainEvents(result.getEvents(), "train1");
		train1events.forEach(System.out::println);

		// calculation of expected time for train1: acceleration = 0.5, length = 100
		// route: - station: 2.77777m/s
		//        - link: 50000m, 13.8889m/s
		//        - station: 200m, 2.777777m/s
		//        - link: 50000m, 13.8889m/s
		//        - station: 200m, 2.777777m/s

		double departureTime = 8 * 3600;
		double trainLength = 100;
		double acceleration = 0.5;
		double stationSpeed = 2.7777777777777777;
		double stationLength = 200;
		double linkSpeed = 13.8889;
		double linkLength = 50000;

		double currentTime = departureTime;
		assertTrainState(currentTime, 0, 0, 0, stationLength, train1events);

		// train starts in the station, accelerates to station speed and continues until the train has left the station link
		assertTrainState(currentTime, 0, stationSpeed, acceleration, 0, train1events);

		double accTime1 = timeToAccelerate(0, stationSpeed, acceleration);
		double accDistance1 = distanceTravelled(0, acceleration, accTime1);
		currentTime += accTime1;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, accDistance1, train1events);

		double cruiseTime2 = timeForDistance(trainLength - accDistance1, stationSpeed);
		double cruiseDistance2 = distanceTravelled(stationSpeed, 0, cruiseTime2); // should be = trainLength - accDistance1
		currentTime += cruiseTime2;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, accDistance1 + cruiseDistance2, train1events);

		// train further accelerates to link speed
		double accTime3 = timeToAccelerate(stationSpeed, linkSpeed, acceleration);
		double accDistance3 = distanceTravelled(stationSpeed, acceleration, accTime3);
		currentTime += accTime3;
		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, accDistance1 + cruiseDistance2 + accDistance3, train1events);

		// train can cruise with link speed until it needs to decelerate for next station
		double decTime5 = timeToAccelerate(linkSpeed, stationSpeed, -acceleration);
		double decDistance5 = distanceTravelled(linkSpeed, -acceleration, decTime5);

		double cruiseDistance4 = linkLength - accDistance1 - cruiseDistance2 - accDistance3 - decDistance5;
		double cruiseTime4 = timeForDistance(cruiseDistance4, linkSpeed);
		currentTime += cruiseTime4;
		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, linkLength - decDistance5, train1events);
		// start deceleration
		assertTrainState(currentTime, linkSpeed, stationSpeed, -acceleration, linkLength - decDistance5, train1events);
		currentTime += decTime5;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, linkLength, train1events);

		// Trains stops at station in the middle, calculated directly
		currentTime = 32524.2;
		assertTrainState(currentTime, 0, 0, 0, stationLength, train1events);

		double accelAfterStation = timeToAccelerate(0, stationSpeed, acceleration);
		double distAfterStation = distanceTravelled(0, acceleration, accelAfterStation);

		currentTime += accelAfterStation;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, distAfterStation, train1events);

		// train passes station completely
		double leaveStation = timeForDistance(trainLength - distAfterStation, stationSpeed);

		currentTime += leaveStation;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, trainLength, train1events);

		// train can accelerate again to link speed
		double accTime7 = timeToAccelerate(stationSpeed, linkSpeed, acceleration);
		double accDistance7 = distanceTravelled(stationSpeed, acceleration, accTime7);
		currentTime += accTime7;
		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, trainLength + accDistance7, train1events);

		// train can cruise with link speed until it needs to decelerate for final station
		double decTime9 = timeToAccelerate(linkSpeed, stationSpeed, -acceleration);
		double decDistance9 = distanceTravelled(linkSpeed, -acceleration, decTime9);

		double cruiseDistance8 = linkLength - trainLength - accDistance7 - decDistance9;
		double cruiseTime8 = timeForDistance(cruiseDistance8, linkSpeed);

		currentTime += cruiseTime8 + decTime9;

		// end of link, entering station
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, linkLength, train1events);

		// train can cruise into station link until it needs to fully brake
		double decTime11 = timeToAccelerate(stationSpeed, 0, -acceleration);
		double decDistance11 = distanceTravelled(stationSpeed, -acceleration, decTime11);

		double cruiseDistance10 = stationLength - decDistance11;
		double cruiseTime10 = timeForDistance(cruiseDistance10, stationSpeed);

		currentTime += cruiseTime10;
		assertTrainState(currentTime, stationSpeed, 0, -acceleration, cruiseDistance10, train1events);

		// final train arrival
		currentTime += decTime11;
		assertTrainState(currentTime, 0, 0, 0, stationLength, train1events);
	}

	@Test
	void testMicroTrackOppositeTraffic() {
		// two trains approach each other, they meet approximately in the middle of the track (double laned track with same resource id "t2_A-t2_B")
		// one train has to wait for the other train to pass, then continues
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTraffic"));
		List<Event> events = result.getEvents();

		// assert that both trains arrive at their final facility at time ("train2" arrives later than "train1" because it waits for "train1" to pass)
		assertThat(result).trainHasLastArrival("train1", 36845.0).trainHasLastArrival("train2", 37253.0);

		// check that train2 waits multiple timesteps for train1 to pass
		List<RailsimTrainStateEvent> train2EventsOnMiddleLink = filterTrainEvents(events, "train2").stream().filter(event -> (event.getHeadLink().toString().equals("t3_A-t2_B"))).toList();
		Assertions.assertTrue(train2EventsOnMiddleLink.stream().filter(event -> (event.getSpeed() == 0.0)).count() > 40);
	}

	@Test
	void testMicroTrackOppositeTrafficMany() {
		// multiple trains, one slow train (train3)
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTrafficMany"));

		// trains 1-4 go from left to right, trains 5-8 go from right to left but wait for trains 1-4 to pass the middle resource
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 36845.0)
			.trainHasLastArrival("train2", 40545.0)
			.trainHasLastArrival("train3", 44245.0)
			.trainHasLastArrival("train4", 47945.0)
			// now the trains from the right side arrive
			.trainHasLastArrival("train5", 51645.0)
			.trainHasLastArrival("train6", 52054.0)
			.trainHasLastArrival("train7", 55755.0)
			.trainHasLastArrival("train8", 59455.0);

	}

	@Test
	void testMesoTwoSources() {
		// several trains start at the same time from two different sources (one having capacity >>1), they meet at a bottleneck with capacity 1
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSources"));
		// on the mesoscopic link there should be multiple trains at the same time
		assertMultipleTrainsOnLink(result.getEvents(), "t1_B-t2_A");
		// check that only one train is on the bottleneck link at a time
		assertSingleTrainOnLink(result.getEvents(), "t2_A-t2_B");

		// The trains can only pass the bottleneck one after another, this the arrival times are spread out
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 29769.0)
			.trainHasLastArrival("train2", 29887.0)
			.trainHasLastArrival("train3", 30007.0)
			.trainHasLastArrival("train4", 30127.0)
			.trainHasLastArrival("train5", 30339.0)
			.trainHasLastArrival("train6", 30789.0);
	}

	@Test
	void testMesoTwoSourcesComplex() {
		// seems redundant to me, this test is simpler than testMesoTwoSources...
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSourcesComplex"));

		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 37358.0)
			.trainHasLastArrival("train2", 41578.0);
	}

	@Test
	void testScenarioMesoGenfBernAllTrains() {
		// this is a complex (realistic) scenario...
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"));

		//assertThat(result).allTrainsArrived(); this fails, some don't arrive...
		assertThat(result).hasNumberOfTrains(210);
		// check the number of events... we could see if something changes significantly in the future
		Assertions.assertEquals(2605247, result.getEvents().size(), 1000);
	}

	@Test
	void testScenarioMesoGenfBernOneTrain() {
		// simplified scenario from the above...Remove vehicles except the first one
		Consumer<Scenario> filter = scenario -> {

			Set<Id<Vehicle>> remove = scenario.getTransitVehicles().getVehicles().values().stream()
				.filter(v -> !v.getId().toString().equals("Bummelzug_GE_BE_train_0")).map(Vehicle::getId).collect(Collectors.toSet());

			for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {

				for (TransitRoute route : line.getRoutes().values()) {

					Collection<Departure> values = new ArrayList<>(route.getDepartures().values());
					for (Departure departure : values) {

						if (remove.contains(departure.getVehicleId())) route.removeDeparture(departure);
					}
				}
			}

			remove.forEach(v -> scenario.getTransitVehicles().removeVehicle(v));
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"), filter);

		assertThat(result).allTrainsArrived().trainHasLastArrival("Bummelzug_GE_BE_train_0", 9221.0);
	}

	@Test
	void testMicroThreeUniDirectionalTracks() {
		// three parallel uni-directional tracks. Train1 starts from the uppermost track and goes to the bottom
		// Train2 stays in middle track and waits for train1 to pass, then continues
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microThreeUniDirectionalTracks"));

		assertThat(result).allTrainsArrived()
			.trainHasLastArrival("train1", 30640.0)
			.trainHasLastArrival("train2", 30531.0);
	}

	@Test
	void testMicroTrainFollowingConstantSpeed() {
		// two train starts from a station (low speed) and accelerate once their tail reaches the end of the station link
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingConstantSpeed"));

		assertThat(result).allTrainsArrived()
			.trainHasLastArrival("train1", 29820.0)
			.trainHasLastArrival("train2", 30111.0);

		// train1 is faster than train2
		// Both trains do not accelerate to their maximum speed but only to targetSpeed
		// targetSpeed << than min(freeSpeed Link, maxSpeed Train) because of train disposition
		filterTrainEvents(result.getEvents(), "train1").forEach(event -> {
			if (event.getHeadLink().toString().equals("12-13")) {
				Assertions.assertEquals(24, event.getSpeed(), 1, "Train1 speed on link is not correct.");
			}
		});

		filterTrainEvents(result.getEvents(), "train2").forEach(event -> {
			if (event.getHeadLink().toString().equals("12-13")) {
				Assertions.assertEquals(11.5, event.getSpeed(), 1, "Train1 speed on link is not correct.");
			}
		});
	}


	@Test
	void testMicroTrainFollowingVaryingSpeed() {
		// This test is similar to testMicroTrainFollowingConstantSpeed but with varying speed levels along the corridor.
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingVaryingSpeed"));

		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 29525.0)
			.trainHasLastArrival("train2", 29782.0);

		// some links have 2m/s limit
		String slowLink = "8-9"; // link with 2m/s limit
		String fastLink = "15-16"; // link with 10m/s limit
		// note: the train will only accelerate when the tail is also in a fast link (which starts 11-12)

		filterTrainEvents(result.getEvents(), "train1").forEach(event -> {
			if (event.getHeadLink().toString().equals(slowLink)) {
				Assertions.assertEquals(result.getScenario().getNetwork().getLinks().get(Id.createLinkId(slowLink)).getFreespeed(), event.getSpeed(), 0.01, "Train1 speed on link is not correct.");
			}

			if (event.getHeadLink().toString().equals(fastLink)) {
				Assertions.assertTrue(event.getSpeed() > result.getScenario().getNetwork().getLinks().get(Id.createLinkId(slowLink)).getFreespeed(), "Train1 speed on link is not correct.");
			}
		});

		filterTrainEvents(result.getEvents(), "train2").forEach(event -> {
			if (event.getHeadLink().toString().equals(slowLink)) {
				Assertions.assertEquals(result.getScenario().getNetwork().getLinks().get(Id.createLinkId(slowLink)).getFreespeed(), event.getSpeed(), 0.01, "Train2 speed on link is not correct.");
			}
			if (event.getHeadLink().toString().equals(fastLink)) {
				Assertions.assertTrue(event.getSpeed() > result.getScenario().getNetwork().getLinks().get(Id.createLinkId(slowLink)).getFreespeed(), "Train2 speed on link is not correct.");
			}
		});
	}

	@Test
	void testMicroTrainFollowingFixedVsMovingBlock() {
		// two parallel tracks, one with fixed block (upper) and one with moving block (lower)
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingFixedVsMovingBlock"));

		assertThat(result).allTrainsArrived()
			.trainHasLastArrival("t1_train1", 30531.0)
			.trainHasLastArrival("t1_train2", 30810.0)
			.trainHasLastArrival("t2_train1", 30531.0)
			.trainHasLastArrival("t2_train2", 30682.0);

		// lower trains which follow first train ("2") arrive sooner (moving block)
		Assertions.assertTrue(result.getStopTimes().get("t1_train2").lastEntry().getValue().arrivalTime > result.getStopTimes().get("t2_train2").lastEntry().getValue().arrivalTime);
	}

	@Test
	void testMicroStationSameLink() {
		// complex (realistic network) with a station at the center. Not sure what this test is supposed to test...
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microStationSameLink"));
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 30152.0)
			.trainHasLastArrival("train2", 30025.0);
	}

	@Test
	void testMicroStationDifferentLink() {
		// Same network as testMicroStationSameLink but the trains take different links at the station.
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microStationDifferentLink"));

		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 29505.0)
			.trainHasLastArrival("train2", 29443.0);
	}

	@Test
	void testMicroJunctionCross() {
		// two trains approach a junction (4 links having same resource id)
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCross"));
		assertThat(result).allTrainsArrived()
			.trainHasLastArrival("train1", 28997.0)
			.trainHasLastArrival("train2", 29068.0);

		// TODO: can we check that only 1 train is on a railsimResourceId? Can't get railsimResourceId out of the Network class
		assertSingleTrainOnLinks(result.getEvents(), List.of("A2", "B2", "A3", "B3"));
	}

	@Test
	void testMicroJunctionFlat() {
		SimulationResult collector = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionFlat"));

		assertThat(collector)
			.trainHasLastArrival("trainAB0", 30884.0)
			.trainHasLastArrival("trainBA0", 30875.0)
			.trainHasLastArrival("trainAC0", 31213.0)
			.trainHasLastArrival("trainCA0", 31027.0)
			.trainHasLastArrival("trainAB7", 31061.0)
			.trainHasLastArrival("trainBA7", 32971.0)
			.trainHasLastArrival("trainAC7", 31447.0)
			.trainHasLastArrival("trainCA7", 31332.0)
			.allTrainsArrived();
	}

	@Test
	void testMicroJunctionY() {
		// two trains apprach a junction from different directions (microscopic), then merge on the same mesoscopic link where
		// train 2 overtakes train 1
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionY"));
		assertThat(result)
			.trainHasLastArrival("train1", 40210.0)
			.trainHasLastArrival("train2", 36207.0)
			.allTrainsArrived();
	}

	@Test
	void testMesoStationCapacityOne() {
		// 3 trains drive towards a station with capacity 1, they pass sequentially
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityOne"));

		assertThat(result)
			.trainHasLastArrival("train1", 29588.0)
			.trainHasLastArrival("train2", 30180.0)
			.trainHasLastArrival("train3", 29856.0)
			.allTrainsArrived();

		assertSingleTrainOnLink(result.getEvents(), "X");
	}

	@Test
	void testMesoStationCapacityTwo() {
		// same as above, but middle link has capacity 3, so two trains could pass at the same time
		// Note, that with deadlock prevention, the capacity needs to be 3 so that 2 trains can pass at the same time
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityTwo"));

		assertThat(result)
			.trainHasLastArrival("train1", 29588.0)
			.trainHasLastArrival("train2", 29972.0)
			.trainHasLastArrival("train3", 29828.0)
			.allTrainsArrived();

		assertMultipleTrainsOnLink(result.getEvents(), "X");
	}

	@Test
	void testMesoStations() {
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStations"));
	}

	@Test
	void testMicroSimpleUniDirectionalTrack() {
		SimulationResult collector = runSimulation(new File(utils.getPackageInputDirectory(), "microSimpleUniDirectionalTrack"));

		for (Event event : collector.getEvents()) {
			if (event.getEventType().equals(VehicleArrivesAtFacilityEvent.EVENT_TYPE)) {

				VehicleArrivesAtFacilityEvent vehicleArrivesEvent = (VehicleArrivesAtFacilityEvent) event;
				if (vehicleArrivesEvent.getVehicleId().toString().equals("train1") && vehicleArrivesEvent.getFacilityId().toString()
					.equals("stop_3-4")) {

					Assertions.assertEquals(29594., event.getTime(), MatsimTestUtils.EPSILON, "The arrival time of train1 at stop_3-4 has changed.");
				}
			}
		}
	}

	@Test
	void testMicroStationRerouting() {
		SimulationResult collector = runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"));
		collector.getEvents().forEach(System.out::println);
		// Checks end times
		assertTrainState(30854, 0, 0, 0, 400, filterTrainEvents(collector.getEvents(), "train1"));
		// 1min later
		assertTrainState(30914, 0, 0, 0, 400, filterTrainEvents(collector.getEvents(), "train2"));
		// These arrive closer together, because both waited
		assertTrainState(30974, 0, 0, 0, 400, filterTrainEvents(collector.getEvents(), "train3"));
		assertTrainState(31034, 0, 0, 0, 400, filterTrainEvents(collector.getEvents(), "train4"));

		// collect all arrivals and departures
		Map<Id<Vehicle>, List<Id<TransitStopFacility>>> train2arrivalStops = new HashMap<>();
		Map<Id<Vehicle>, List<Id<TransitStopFacility>>> train2departureStops = new HashMap<>();
		Map<Id<Vehicle>, List<RailsimDetourEvent>> train2detours = new HashMap<>();

		for (Event event : collector.getEvents()) {
			if (event instanceof VehicleArrivesAtFacilityEvent vehicleArrivesEvent) {
				train2arrivalStops.computeIfAbsent(vehicleArrivesEvent.getVehicleId(), k -> new ArrayList<>())
					.add(vehicleArrivesEvent.getFacilityId());
			}

			if (event instanceof VehicleDepartsAtFacilityEvent vehicleDepartsEvent) {
				train2departureStops.computeIfAbsent(vehicleDepartsEvent.getVehicleId(), k -> new ArrayList<>())
					.add(vehicleDepartsEvent.getFacilityId());
			}

			if (event instanceof RailsimDetourEvent detour) {
				train2detours.computeIfAbsent(detour.getVehicleId(), k -> new ArrayList<>()).add(detour);
			}
		}

		// test if all trains have the correct number of arrivals and departures
		for (Id<Vehicle> vehicleId : train2arrivalStops.keySet()) {
			Assertions.assertEquals(3, train2arrivalStops.get(vehicleId).size(), MatsimTestUtils.EPSILON, "Wrong number of arrival stops for train " + vehicleId);
		}
		for (Id<Vehicle> vehicleId : train2departureStops.keySet()) {
			Assertions.assertEquals(3, train2departureStops.get(vehicleId).size(), MatsimTestUtils.EPSILON, "Wrong number of departure stops for train " + vehicleId);
		}

		// test if the trains have the correct arrival / departure stop facilities
		Assertions.assertEquals("AB", train2arrivalStops.get(Id.createVehicleId("train3")).get(0).toString(), "Wrong stop facility. This is the start stop facility.");

		// The original events don't contain the re-routing. They appear to other agents as if the train drove the original schedule.
		Assertions.assertEquals("CE", train2arrivalStops.get(Id.createVehicleId("train3")).get(1).toString(), "Wrong stop facility. This is the original id");
		// Detour facility
		Assertions.assertEquals("CD", train2detours.get(Id.createVehicleId("train3")).get(0).getNewStop().toString());


		Assertions.assertEquals("JK", train2arrivalStops.get(Id.createVehicleId("train3")).get(2).toString(), "Wrong stop facility. This is the final stop facility.");
	}

	@Test
	void testMicroStationReroutingConcurrent() {
		Consumer<Scenario> filter = scenario -> {

			TransitScheduleFactory f = scenario.getTransitSchedule().getFactory();

			for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {

				for (TransitRoute route : line.getRoutes().values()) {

					Collection<Departure> values = new ArrayList<>(route.getDepartures().values());
					values.forEach(route::removeDeparture);

					// Re-create departure for same time
					for (Departure departure : values) {
						Departure d = f.createDeparture(departure.getId(), 8 * 3600);
						d.setVehicleId(departure.getVehicleId());
						route.addDeparture(d);
					}
				}
			}
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"), filter);
	}

	@Test
	void testMicroStationReroutingTwoDirections() {
		modifyScheduleAndRunSimulation(new File(utils.getPackageInputDirectory(), "microStationReroutingTwoDirections"), 300, 900);

//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Future<?> future = executor.submit(() -> {
//            modifyScheduleAndRunSimulation(
//                new File(utils.getPackageInputDirectory(), "microStationReroutingTwoDirections"),
//                300,
//                1800
//            );
//        });
//
//        try {
//            future.get(30, TimeUnit.SECONDS);
//        } catch (TimeoutException e) {
//            future.cancel(true);
//            fail("Simulation took too long and was aborted!");
//        } catch (ExecutionException | InterruptedException e) {
//        	// other exceptions
//        	fail("An error occurred during the simulation: " + e.getMessage());
//        } finally {
//            executor.shutdownNow();
//        }
	}

	@Test
	void testScenarioKelheim() {

		// This tests runs railsim on a conventional pt network, in a standard scenario.

		URL base = ExamplesUtils.getTestScenarioURL("kelheim");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(base, "config.xml"));
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Convert all pt to rail
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) link.setAllowedModes(Set.of(TransportMode.car, "ride", "freight"));

			if (link.getAllowedModes().contains(TransportMode.pt)) {
				link.setFreespeed(50);
				link.setAllowedModes(Set.of("rail"));
			}
		}

		// Maximum velocity must be configured
		for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
			type.setMaximumVelocity(30);
			type.setLength(100);
		}

		// simplify the activity types, e.g. home_3600 -> home
		Set<String> activityTypes = new HashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity act) {
						String baseType = act.getType().split("_")[0];
						act.setType(baseType);
						activityTypes.add(baseType);
					}
				}
			}
		}

		for (String type : activityTypes) {
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(1234.));
		}

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		EventsCollector collector = new EventsCollector();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(collector);
			}
		});

		controler.run();
		SimulationResult result = new SimulationResult(scenario, collector);

		assertThat(result)
			.allTrainsArrived();
	}

	@Test
	void testScenarioMicroMesoCombination() {
		// rather complex scenario with a combination of micro and mesoscopic links
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMicroMesoCombination"));

		// just checking times...
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("v_CARGO_1", 29030.0)
			.trainHasLastArrival("v_CARGO_2", 29032.0)
			.trainHasLastArrival("v_IC_1", 29583.0)
			.trainHasLastArrival("v_IC_2", 29650.0)
			.trainHasLastArrival("v_IC_3", 31383.0)
			.trainHasLastArrival("v_IC_4", 31450.0)
			.trainHasLastArrival("v_IR_1", 30321.0)
			.trainHasLastArrival("v_IR_2", 30609.0)
			.trainHasLastArrival("v_IR_3", 32121.0)
			.trainHasLastArrival("v_IR_4", 32409.0);

	}

	@Test
	void testScenarioMicroMesoConstructionSiteLsGe() {
		// two parallel tracks with opposite traffic...
		// TODO: I don't see the purpose of this test
		// TODO: note that we have two links from="MOR2" to="REN1", is this on purpose?
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMicroMesoConstructionSiteLsGe"));

		assertThat(result).hasNumberOfTrains(71).allTrainsArrived();
		Assertions.assertEquals(result.getEvents().size(), 31731, 10);
	}

	private void assertSingleTrainOnLink(List<Event> events, String linkId) {
		assertSingleTrainOnLinks(events, List.of(linkId));
	}

	private void assertSingleTrainOnLinks(List<Event> events, List<String> linkIds) {
		// Group events by time and check for multiple trains on the same set of links at the same time
		Map<Double, List<RailsimTrainStateEvent>> eventsByTime = events.stream()
			.filter(e -> e instanceof RailsimTrainStateEvent)
			.map(e -> (RailsimTrainStateEvent) e)
			.filter(e -> linkIds.contains(e.getHeadLink().toString()) || linkIds.contains(e.getTailLink().toString()))
			.collect(Collectors.groupingBy(Event::getTime));

		for (Map.Entry<Double, List<RailsimTrainStateEvent>> entry : eventsByTime.entrySet()) {
			Set<Id<Vehicle>> trainsOnLinks = entry.getValue().stream()
				.map(RailsimTrainStateEvent::getVehicleId)
				.collect(Collectors.toSet());

			if (trainsOnLinks.size() > 1) {
				List<Id<Vehicle>> vehicleIds = new ArrayList<>(trainsOnLinks);
				Assertions.fail("Multiple trains " + vehicleIds + " on links " + linkIds + " at time " + entry.getKey());
			}
		}
	}

	private void assertMultipleTrainsOnLink(List<Event> events, String linkId) {
		// Group events by time to check for multiple trains on the same link at the same time
		Map<Double, List<RailsimTrainStateEvent>> eventsByTime = events.stream()
			.filter(e -> e instanceof RailsimTrainStateEvent)
			.map(e -> (RailsimTrainStateEvent) e)
			.filter(e -> e.getHeadLink().toString().equals(linkId) || e.getTailLink().toString().equals(linkId))
			.collect(Collectors.groupingBy(Event::getTime));

		for (Map.Entry<Double, List<RailsimTrainStateEvent>> entry : eventsByTime.entrySet()) {
			Set<Id<Vehicle>> trainsOnLink = entry.getValue().stream()
				.map(RailsimTrainStateEvent::getVehicleId)
				.collect(Collectors.toSet());
			if (trainsOnLink.size() > 1) {
				// Found a time with multiple trains, assertion passes
				return;
			}
		}

		Assertions.fail("No instance found with multiple trains on link " + linkId + " at the same time.");
	}
}
