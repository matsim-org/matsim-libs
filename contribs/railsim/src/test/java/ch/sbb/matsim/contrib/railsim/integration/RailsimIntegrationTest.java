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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.events.RailsimDetourEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;

public class RailsimIntegrationTest extends AbstractIntegrationTest {

	@Test
	void testMicroSimpleBiDirectionalTrack() {
		// We have 6 trains going sequantially from t1 to t3.
		List<Event> events = runSimulation(new File(utils.getPackageInputDirectory(), "microSimpleBiDirectionalTrack")).getEvents();

		// assert that all of them arrived at the final station (facility t3) and check their travel duration.
		for (String train : List.of("train1", "train2", "train3", "train4", "train5", "train6")) {
			// check if the train arrived at the final station
			List<VehicleArrivesAtFacilityEvent> arrivals = filterVehicleArrivesAtFacilityEvent(events, train);
			Assertions.assertTrue(arrivals.stream().anyMatch(event -> event.getFacilityId().toString().equals("t3")),
				"Train " + train + " did not arrive at the final station t3.");

			double duration = getTravelDuration(events, train, "t1", "t3");
			Assertions.assertEquals(380.0, duration, 1.0,
				"Train " + train + " did not have the expected travel duration. Expected 380s, but was " + duration + "s.");
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
		List<Event> events = runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTraffic")).getEvents();

		// assert that both trains arrive at their final facility
		Assertions.assertTrue(filterVehicleArrivesAtFacilityEvent(events, "train1").stream().anyMatch(event -> (event.getFacilityId().toString().equals("t3_A-B"))),
			"Train1 did not arrive at the final facility t3_A-B.");

		Assertions.assertTrue(filterVehicleArrivesAtFacilityEvent(events, "train2").stream().anyMatch(event -> (event.getFacilityId().toString().equals("t1_B-A"))),
			"Train2 did not arrive at the final facility t1_B-A.");

		// check that train2 waits multiple /events for train1 to pass
		List<RailsimTrainStateEvent> train2EventsOnMiddleLink = filterTrainEvents(events, "train2").stream().filter(event -> (event.getHeadLink().toString().equals("t3_A-t2_B"))).toList();
		Assertions.assertTrue(train2EventsOnMiddleLink.stream().filter(event -> (event.getSpeed() == 0.0)).count() > 40);

	}

	@Test
	void testMicroTrackOppositeTrafficMany() {
		// multiple trains, one slow train
		runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTrafficMany"));
	}

	@Test
	void testMesoTwoSources() {
		runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSources"));
	}

	@Test
	void testMesoTwoSourcesComplex() {
		runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSourcesComplex"));
	}

	@Test
	void testScenarioMesoGenfBernAllTrains() {
		runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"));
	}

	@Test
	void testScenarioMesoGenfBernOneTrain() {

		// Remove vehicles except the first one
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

		runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"), filter);

	}

	@Test
	void testMicroThreeUniDirectionalTracks() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microThreeUniDirectionalTracks"));
	}

	@Test
	void testMicroTrainFollowingConstantSpeed() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingConstantSpeed"));
	}

	// This test is similar to testMicroTrainFollowingConstantSpeed but with varying speed levels along the corridor.
	@Test
	void testMicroTrainFollowingVaryingSpeed() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingVaryingSpeed"));
	}

	@Test
	void testMicroTrainFollowingFixedVsMovingBlock() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingFixedVsMovingBlock"));
	}

	@Test
	void testMicroStationSameLink() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microStationSameLink"));
	}

	@Test
	void testMicroStationDifferentLink() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microStationDifferentLink"));
	}

	@Test
	void testMicroJunctionCross() {
		SimulationResult collector = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCross"));
		// check if the train arrives at the stop
		boolean vehicleArrivesAtFacilityEventFound1 = false;
		boolean vehicleArrivesAtFacilityEventFound2 = false;

		for (Event event : collector.getEvents()) {
			if (event.getEventType().equals(VehicleArrivesAtFacilityEvent.EVENT_TYPE)) {

				VehicleArrivesAtFacilityEvent vehicleArrivesEvent = (VehicleArrivesAtFacilityEvent) event;

				if (vehicleArrivesEvent.getVehicleId().toString().equals("train1") && vehicleArrivesEvent.getFacilityId().toString()
					.equals("stop_A5")) {
					vehicleArrivesAtFacilityEventFound1 = true;
					// TODO: also test the arrival time
//					Assertions.assertEquals(29594., event.getTime(), MatsimTestUtils.EPSILON, "The arrival time of train1 at stop_3-4 has changed.");
				}

				if (vehicleArrivesEvent.getVehicleId().toString().equals("train2") && vehicleArrivesEvent.getFacilityId().toString()
						.equals("stop_B5")) {
						vehicleArrivesAtFacilityEventFound2 = true;
					// TODO: also test the arrival time
//					Assertions.assertEquals(29594., event.getTime(), MatsimTestUtils.EPSILON, "The arrival time of train1 at stop_3-4 has changed.");
				}
			}
		}
		Assertions.assertTrue(vehicleArrivesAtFacilityEventFound1, "VehicleArrivesAtFacilityEvent for train1 at stop_A5 not found.");
		Assertions.assertTrue(vehicleArrivesAtFacilityEventFound2, "VehicleArrivesAtFacilityEvent for train2 at stop_B5 not found.");

	}

	@Test
	void testMicroJunctionFlat() {
		SimulationResult collector = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionFlat"));

		assertThat(collector)
			.trainHasLastArrival("trainAB0", 30884.0)
			.trainHasLastArrival("trainBA0", 30875.0)
			.trainHasLastArrival("trainAC0", 31443.0)
			.trainHasLastArrival("trainCA0", 31027.0)
			.trainHasLastArrival("trainAB7", 31061.0)
			.trainHasLastArrival("trainBA7", 32221.0)
			.trainHasLastArrival("trainAC7", 32184.0)
			.trainHasLastArrival("trainCA7", 33122.0)
			.allTrainsArrived();

	}

	@Test
	void testMicroJunctionY() {
		runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionY"));
	}

	@Test
	void testMesoStationCapacityOne() {
		runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityOne"));
	}

	@Test
	void testMesoStationCapacityTwo() {
		runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityTwo"));
	}

	@Test
	void testMesoStations() {
		runSimulation(new File(utils.getPackageInputDirectory(), "mesoStations"));
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

		runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"), filter);
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
					if(pE instanceof Activity) {
						Activity act = (Activity) pE;
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

		controler.run();
	}

	@Test
	void testScenarioMicroMesoCombination() {
		runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMicroMesoCombination"));
	}

	@Test
	void testScenarioMicroMesoConstructionSiteLsGe() {
		runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMicroMesoConstructionSiteLsGe"));
	}

	private List<VehicleArrivesAtFacilityEvent> filterVehicleArrivesAtFacilityEvent(List<Event> events, String train) {
		return events.stream().filter(event -> event instanceof VehicleArrivesAtFacilityEvent).map(event -> (VehicleArrivesAtFacilityEvent) event)
			.filter(event -> event.getVehicleId().toString().equals(train)).toList();
	}

	private List<VehicleDepartsAtFacilityEvent> filterVehicleDepartsAtFacilityEvent(List<Event> events, String train) {
		return events.stream().filter(event -> event instanceof VehicleDepartsAtFacilityEvent).map(event -> (VehicleDepartsAtFacilityEvent) event)
			.filter(event -> event.getVehicleId().toString().equals(train)).toList();
	}

	private double getTravelDuration(List<Event> events, String train, String startFacility, String endFacility) {
		double departureTime = filterVehicleDepartsAtFacilityEvent(events, train).stream()
			.filter(event -> event.getFacilityId().toString().equals(startFacility)).findFirst().orElseThrow(() -> new RuntimeException("No departure found for train " + train + " at facility " + startFacility)).getTime();

		double arrivalTime = filterVehicleArrivesAtFacilityEvent(events, train).stream()
			.filter(event -> event.getFacilityId().toString().equals(endFacility)).findFirst().orElseThrow(() -> new RuntimeException("No arrival found for train " + train + " at facility " + endFacility)).getTime();

		return arrivalTime - departureTime;
	}
}
