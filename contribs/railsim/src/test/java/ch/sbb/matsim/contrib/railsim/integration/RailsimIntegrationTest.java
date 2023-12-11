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

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RailsimIntegrationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testMicroSimpleBiDirectionalTrack() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microSimpleBiDirectionalTrack"));
	}

	@Test
	void testMesoUniDirectionalVaryingCapacities() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoUniDirectionalVaryingCapacities"));

		// print events of train1 for debugging
		List<RailsimTrainStateEvent> train1events = filterTrainEvents(collector, "train1");
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
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTraffic"));
	}

	@Test
	void testMicroTrackOppositeTrafficMany() {
		// multiple trains, one slow train
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microTrackOppositeTrafficMany"));
	}

	@Test
	void testMesoTwoSources() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSources"));
	}

	@Test
	void testMesoTwoSourcesComplex() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoTwoSourcesComplex"));
	}

	@Test
	void testScenarioMesoGenfBernAllTrains() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"));
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

		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMesoGenfBern"), filter);

	}

	@Test
	void testMicroThreeUniDirectionalTracks() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microThreeUniDirectionalTracks"));
	}

	@Test
	void testMicroTrainFollowingConstantSpeed() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingConstantSpeed"));
	}

	// This test is similar to testMicroTrainFollowingConstantSpeed but with varying speed levels along the corridor.
	@Test
	void testMicroTrainFollowingVaryingSpeed() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingVaryingSpeed"));
	}

	@Test
	void testMicroStationSameLink() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microStationSameLink"));
	}

	@Test
	void testMicroStationDifferentLink() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microStationDifferentLink"));
	}

	@Test
	void testMicroJunctionCross() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCross"));
	}

	@Test
	void testMicroJunctionY() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionY"));
	}

	@Test
	void testMesoStationCapacityOne() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityOne"));
	}

	@Test
	void testMesoStationCapacityTwo() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStationCapacityTwo"));
	}

	@Test
	void testMesoStations() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "mesoStations"));
	}

	@Test
	void testMicroSimpleUniDirectionalTrack() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microSimpleUniDirectionalTrack"));

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
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"));
		collector.getEvents().forEach(System.out::println);
		// Checks end times
		assertTrainState(30854, 0, 0, 0, 400, filterTrainEvents(collector, "train1"));
		// 1min later
		assertTrainState(30914, 0, 0, 0, 400, filterTrainEvents(collector, "train2"));
		// These arrive closer together, because both waited
		assertTrainState(30974, 0, 0, 0, 400, filterTrainEvents(collector, "train3"));
		assertTrainState(30982, 0, 0, 0, 400, filterTrainEvents(collector, "train4"));
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

		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"), filter);
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

		SnzActivities.addScoringParams(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();
	}

	@Test
	void testScenarioMicroMesoCombination() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "scenarioMicroMesoCombination"));
	}

	private EventsCollector runSimulation(File scenarioDir) {
		return runSimulation(scenarioDir, null);
	}

	private EventsCollector runSimulation(File scenarioDir, Consumer<Scenario> f) {
		Config config = ConfigUtils.loadConfig(new File(scenarioDir, "config.xml").toString());

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setDumpDataAtEnd(true);
		config.controller().setCreateGraphs(false);
		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (f != null) f.accept(scenario);

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

		return collector;
	}

	private double timeToAccelerate(double v0, double v, double a) {
		return (v - v0) / a;
	}

	private double distanceTravelled(double v0, double a, double t) {
		return 0.5 * a * t * t + v0 * t;
	}

	private double timeForDistance(double d, double v) {
		return d / v;
	}

	private void assertTrainState(double time, double speed, double targetSpeed, double acceleration, double headPosition, List<RailsimTrainStateEvent> events) {

		RailsimTrainStateEvent prev = null;
		for (RailsimTrainStateEvent event : events) {

			if (event.getTime() > Math.ceil(time)) {
				Assertions.fail(
					String.format("No matching event found for time %f, speed %f pos %f, Closest event is%s", time, speed, headPosition, prev));
			}

			// If all assertions are true, returns successfully
			try {
				Assertions.assertEquals(Math.ceil(time), event.getTime(), 1e-7);
				Assertions.assertEquals(speed, event.getSpeed(), 1e-5);
				Assertions.assertEquals(targetSpeed, event.getTargetSpeed(), 1e-7);
				Assertions.assertEquals(acceleration, event.getAcceleration(), 1e-5);
				Assertions.assertEquals(headPosition, event.getHeadPosition(), 1e-5);
				return;
			} catch (AssertionError e) {
				// Check further events in loop
			}

			prev = event;
		}
	}

	private List<RailsimTrainStateEvent> filterTrainEvents(EventsCollector collector, String train) {
		return collector.getEvents().stream().filter(event -> event instanceof RailsimTrainStateEvent).map(event -> (RailsimTrainStateEvent) event)
			.filter(event -> event.getVehicleId().toString().equals(train)).toList();
	}
}
