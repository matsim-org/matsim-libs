/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleValidatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.pt.utils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;

public class TransitScheduleValidatorTest {

	@Test
	void testPtTutorial() {
		Scenario scenario = ScenarioUtils.loadScenario(
				ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(
				scenario.getTransitSchedule(), scenario.getNetwork());
		assertThat(validationResult.getIssues()).isEmpty();
	}

	@Test
	void testPtTutorialWithError() {
		Scenario scenario = ScenarioUtils.loadScenario(
				ConfigUtils.loadConfig("test/scenarios/pt-tutorial/0.config.xml"));
		TransitLine transitLine = scenario.getTransitSchedule()
				.getTransitLines()
				.get(Id.create("Blue Line", TransitLine.class));
		transitLine.getRoutes()
				.get(Id.create("3to1", TransitRoute.class))
				.getRoute()
				.setLinkIds(Id.createLinkId("33"), Collections.<Id<Link>>emptyList(), Id.createLinkId("11"));
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(
				scenario.getTransitSchedule(), scenario.getNetwork());
		System.out.println(validationResult);

		assertThat(validationResult.getIssues()).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(new TransitScheduleValidator.ValidationResult.ValidationIssue<>(
						TransitScheduleValidator.ValidationResult.Severity.ERROR,
						"Transit line Blue Line, route 3to1: Stop 2b cannot be reached along network route.",
						TransitScheduleValidator.ValidationResult.Type.ROUTE_HAS_UNREACHABLE_STOP,
						List.of(Id.create("2b", TransitStopFacility.class))));
	}

	@Test
	void testValidator_Transfers_implausibleTime() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		Id<TransitStopFacility> id1 = Id.create(1, TransitStopFacility.class);
		Id<TransitStopFacility> id2 = Id.create(2, TransitStopFacility.class);
		Id<TransitStopFacility> id3 = Id.create(3, TransitStopFacility.class);
		Id<TransitStopFacility> id4 = Id.create(4, TransitStopFacility.class);

		schedule.addStopFacility(factory.createTransitStopFacility(id1, new Coord(10000, 10000), false));
		schedule.addStopFacility(factory.createTransitStopFacility(id2, new Coord(20000, 10000), false));

		schedule.getMinimalTransferTimes().set(id1, id2, 120);
		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateTransfers(schedule);
		Assertions.assertTrue(result.getIssues().isEmpty());

		schedule.getMinimalTransferTimes().set(id1, id2, 0);
		result = TransitScheduleValidator.validateTransfers(schedule);
		Assertions.assertEquals(1, result.getIssues().size(), "Should warn against implausible transfer time.");
	}

	@Test
	void testValidator_Transfers_missingStop() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		Id<TransitStopFacility> id1 = Id.create("stop1", TransitStopFacility.class);
		Id<TransitStopFacility> id2 = Id.create("stop2", TransitStopFacility.class);
		Id<TransitStopFacility> id3 = Id.create("stop3", TransitStopFacility.class);
		Id<TransitStopFacility> id4 = Id.create("stop4", TransitStopFacility.class);

		schedule.addStopFacility(factory.createTransitStopFacility(id1, new Coord(10000, 10000), false));
		schedule.addStopFacility(factory.createTransitStopFacility(id2, new Coord(20000, 10000), false));

		schedule.getMinimalTransferTimes().set(id1, id3, 120);
		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateTransfers(schedule);
		Assertions.assertEquals(1, result.getIssues().size(), "Should warn against missing stop3.");
		Assertions.assertTrue(result.getIssues().get(0).getMessage().contains("stop3"), "Message should contain hint about stop3 being missing. " + result.getIssues().get(0).getMessage());
		schedule.getMinimalTransferTimes().remove(id1, id3);

		schedule.getMinimalTransferTimes().set(id4, id2, 120);
		result = TransitScheduleValidator.validateTransfers(schedule);
		Assertions.assertEquals(1, result.getIssues().size(), "Should warn against missing stop4.");
		Assertions.assertTrue(result.getIssues().get(0).getMessage().contains("stop4"), "Message should contain hint about stop4 being missing. " + result.getIssues().get(0).getMessage());
	}

	@Test
	void testValidator_ChainedDepartures_example() {

		String inputFile = "test/input/org/matsim/pt/transitSchedule/chained_departures_schedule.xml";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReader(scenario).readFile(inputFile);

		ValidationResult validationResult = TransitScheduleValidator.validateChainedDepartures(schedule);

		assertThat(validationResult.isValid()).isTrue();
	}

	@Test
	void testValidator_ChainedDepartures_valid() {
		// Create a scenario with valid chained departures
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		// Create transit lines and routes
		Id<TransitLine> line1Id = Id.create("line1", TransitLine.class);
		Id<TransitRoute> route1Id = Id.create("route1", TransitRoute.class);

		TransitLine line1 = factory.createTransitLine(line1Id);
		TransitRoute route1 = factory.createTransitRoute(route1Id, null, Collections.emptyList(), "bus");

		// Create departures
		Id<Departure> dep1Id = Id.create("dep1", Departure.class);
		Id<Departure> dep2Id = Id.create("dep2", Departure.class);

		Departure departure1 = factory.createDeparture(dep1Id, 8.0 * 3600);
		Departure departure2 = factory.createDeparture(dep2Id, 9.0 * 3600);

		// Create a chained departure from dep1 to dep2
		ChainedDeparture chainedDep = factory.createChainedDeparture(line1Id, route1Id, dep2Id);
		departure1.setChainedDepartures(List.of(chainedDep));

		// Add departures to route
		route1.addDeparture(departure1);
		route1.addDeparture(departure2);

		// Add route to line and line to schedule
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		// Validate chained departures
		ValidationResult result = TransitScheduleValidator.validateChainedDepartures(schedule);

		// Should have no issues
		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	void testValidator_ChainedDepartures_missingLine() {
		// Create a scenario with an invalid line reference in a chained departure
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		// Create transit lines and routes
		Id<TransitLine> line1Id = Id.create("line1", TransitLine.class);
		Id<TransitLine> missingLineId = Id.create("missingLine", TransitLine.class);
		Id<TransitRoute> route1Id = Id.create("route1", TransitRoute.class);

		TransitLine line1 = factory.createTransitLine(line1Id);
		TransitRoute route1 = factory.createTransitRoute(route1Id, null, Collections.emptyList(), "bus");

		// Create departures
		Id<Departure> dep1Id = Id.create("dep1", Departure.class);
		Id<Departure> dep2Id = Id.create("dep2", Departure.class);

		Departure departure1 = factory.createDeparture(dep1Id, 8.0 * 3600);
		Departure departure2 = factory.createDeparture(dep2Id, 9.0 * 3600);

		// Create a chained departure with a reference to a missing line
		ChainedDeparture chainedDep = factory.createChainedDeparture(missingLineId, route1Id, dep2Id);
		departure1.setChainedDepartures(List.of(chainedDep));

		// Add departures to route
		route1.addDeparture(departure1);
		route1.addDeparture(departure2);

		// Add route to line and line to schedule
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		// Validate chained departures
		ValidationResult result = TransitScheduleValidator.validateChainedDepartures(schedule);

		// Should have 1 error for the missing line
		assertThat(result.getIssues()).hasSize(1);
		ValidationResult.ValidationIssue issue = result.getIssues().get(0);
		assertThat(issue.getSeverity()).isEqualTo(ValidationResult.Severity.ERROR);
		assertThat(issue.getMessage()).contains("missingLine");
		assertThat(issue.getMessage()).contains("does not exist in the schedule");
	}

	@Test
	void testValidator_ChainedDepartures_missingRoute() {
		// Create a scenario with an invalid route reference in a chained departure
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		// Create transit lines and routes
		Id<TransitLine> line1Id = Id.create("line1", TransitLine.class);
		Id<TransitRoute> route1Id = Id.create("route1", TransitRoute.class);
		Id<TransitRoute> missingRouteId = Id.create("missingRoute", TransitRoute.class);

		TransitLine line1 = factory.createTransitLine(line1Id);
		TransitRoute route1 = factory.createTransitRoute(route1Id, null, Collections.emptyList(), "bus");

		// Create departures
		Id<Departure> dep1Id = Id.create("dep1", Departure.class);
		Id<Departure> dep2Id = Id.create("dep2", Departure.class);

		Departure departure1 = factory.createDeparture(dep1Id, 8.0 * 3600);
		Departure departure2 = factory.createDeparture(dep2Id, 9.0 * 3600);

		// Create a chained departure with a reference to a missing route
		ChainedDeparture chainedDep = factory.createChainedDeparture(line1Id, missingRouteId, dep2Id);
		departure1.setChainedDepartures(List.of(chainedDep));

		// Add departures to route
		route1.addDeparture(departure1);
		route1.addDeparture(departure2);

		// Add route to line and line to schedule
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		// Validate chained departures
		ValidationResult result = TransitScheduleValidator.validateChainedDepartures(schedule);

		// Should have 1 error for the missing route
		assertThat(result.getIssues()).hasSize(1);
		ValidationResult.ValidationIssue issue = result.getIssues().get(0);
		assertThat(issue.getSeverity()).isEqualTo(ValidationResult.Severity.ERROR);
		assertThat(issue.getMessage()).contains("missingRoute");
		assertThat(issue.getMessage()).contains("does not exist in the schedule");
	}

	@Test
	void testValidator_ChainedDepartures_missingDeparture() {
		// Create a scenario with an invalid departure reference in a chained departure
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		// Create transit lines and routes
		Id<TransitLine> line1Id = Id.create("line1", TransitLine.class);
		Id<TransitRoute> route1Id = Id.create("route1", TransitRoute.class);

		TransitLine line1 = factory.createTransitLine(line1Id);
		TransitRoute route1 = factory.createTransitRoute(route1Id, null, Collections.emptyList(), "bus");

		// Create departures
		Id<Departure> dep1Id = Id.create("dep1", Departure.class);
		Id<Departure> missingDepId = Id.create("missingDep", Departure.class);

		Departure departure1 = factory.createDeparture(dep1Id, 8.0 * 3600);

		// Create a chained departure with a reference to a missing departure
		ChainedDeparture chainedDep = factory.createChainedDeparture(line1Id, route1Id, missingDepId);
		departure1.setChainedDepartures(List.of(chainedDep));

		// Add departure to route
		route1.addDeparture(departure1);

		// Add route to line and line to schedule
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		// Validate chained departures
		ValidationResult result = TransitScheduleValidator.validateChainedDepartures(schedule);

		// Should have 1 error for the missing departure
		assertThat(result.getIssues()).hasSize(1);
		ValidationResult.ValidationIssue issue = result.getIssues().get(0);
		assertThat(issue.getSeverity()).isEqualTo(ValidationResult.Severity.ERROR);
		assertThat(issue.getMessage()).contains("missingDep");
		assertThat(issue.getMessage()).contains("does not exist in the schedule");
	}

	@Test
	void testValidator_ChainedDepartures_stopMismatch() {
		// Create a scenario with mismatched stops in chained departures
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		// Create transit lines and routes
		Id<TransitLine> line1Id = Id.create("line1", TransitLine.class);
		Id<TransitRoute> route1Id = Id.create("route1", TransitRoute.class);
		Id<TransitRoute> route2Id = Id.create("route2", TransitRoute.class);

		TransitLine line1 = factory.createTransitLine(line1Id);
		
		// Create stop facilities
		Id<TransitStopFacility> stop1Id = Id.create("stop1", TransitStopFacility.class);
		Id<TransitStopFacility> stop2Id = Id.create("stop2", TransitStopFacility.class);
		Id<TransitStopFacility> stop3Id = Id.create("stop3", TransitStopFacility.class);
		
		TransitStopFacility stop1 = factory.createTransitStopFacility(stop1Id, new Coord(0, 0), false);
		TransitStopFacility stop2 = factory.createTransitStopFacility(stop2Id, new Coord(1000, 0), false);
		TransitStopFacility stop3 = factory.createTransitStopFacility(stop3Id, new Coord(2000, 0), false);
		
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		// Create routes with different stops
		List<TransitRouteStop> route1Stops = new ArrayList<>();
		route1Stops.add(factory.createTransitRouteStop(stop1, 0, 0));
		route1Stops.add(factory.createTransitRouteStop(stop2, 100, 100));
		
		List<TransitRouteStop> route2Stops = new ArrayList<>();
		route2Stops.add(factory.createTransitRouteStop(stop3, 0, 0)); // Different first stop than route1's last stop
		route2Stops.add(factory.createTransitRouteStop(stop1, 100, 100));

		TransitRoute route1 = factory.createTransitRoute(route1Id, null, route1Stops, "bus");
		TransitRoute route2 = factory.createTransitRoute(route2Id, null, route2Stops, "bus");

		// Create departures
		Id<Departure> dep1Id = Id.create("dep1", Departure.class);
		Id<Departure> dep2Id = Id.create("dep2", Departure.class);

		Departure departure1 = factory.createDeparture(dep1Id, 8.0 * 3600);
		Departure departure2 = factory.createDeparture(dep2Id, 9.0 * 3600);

		// Create a chained departure with mismatched stops
		ChainedDeparture chainedDep = factory.createChainedDeparture(line1Id, route2Id, dep2Id);
		departure1.setChainedDepartures(List.of(chainedDep));

		// Add departures to routes
		route1.addDeparture(departure1);
		route2.addDeparture(departure2);

		// Add routes to line and line to schedule
		line1.addRoute(route1);
		line1.addRoute(route2);
		schedule.addTransitLine(line1);

		// Validate chained departures
		ValidationResult result = TransitScheduleValidator.validateChainedDepartures(schedule);

		// Should have 1 error for the stop mismatch
		assertThat(result.getIssues()).hasSize(1);
		ValidationResult.ValidationIssue issue = result.getIssues().get(0);
		assertThat(issue.getSeverity()).isEqualTo(ValidationResult.Severity.ERROR);
		assertThat(issue.getMessage()).contains("does not match the first stop");
		assertThat(issue.getMessage()).contains("stop2");
		assertThat(issue.getMessage()).contains("stop3");
	}
}
