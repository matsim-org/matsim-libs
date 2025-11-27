/* *********************************************************************** *
 * project: org.matsim.*
 * MinimumStopDurationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Test for the minimumStopDuration attribute in transit route stops.
 *
 * @author rakow
 */
public class MinimumStopDurationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadWriteMinimumStopDuration(@TempDir File tempDir) throws IOException {
		// Create a simple scenario with network and transit schedule
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();

		// Create network nodes and links
		NetworkFactory nf = network.getFactory();
		Node node1 = nf.createNode(Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = nf.createNode(Id.create("2", Node.class), new Coord(1000, 0));
		Link link1 = nf.createLink(Id.create("1", Link.class), node1, node2);
		link1.setLength(1000);
		link1.setFreespeed(10);
		network.addNode(node1);
		network.addNode(node2);
		network.addLink(link1);

		// Create transit stop facilities
		TransitStopFacility stop1 = schedule.getFactory().createTransitStopFacility(
				Id.create("stop1", TransitStopFacility.class), new Coord(0, 0), false);
		stop1.setLinkId(Id.create("1", Link.class));
		TransitStopFacility stop2 = schedule.getFactory().createTransitStopFacility(
				Id.create("stop2", TransitStopFacility.class), new Coord(1000, 0), false);
		stop2.setLinkId(Id.create("1", Link.class));
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);

		// Create transit route stops with minimum stop duration
		TransitRouteStop routeStop1 = new TransitRouteStopImpl.Builder()
				.stop(stop1)
				.arrivalOffset(0)
				.departureOffset(30)
				.minimumStopDuration(30)
				.build();
		TransitRouteStop routeStop2 = new TransitRouteStopImpl.Builder()
				.stop(stop2)
				.arrivalOffset(120)
				.departureOffset(150)
				.minimumStopDuration(60)
				.build();

		// Create transit route
		TransitRoute route = schedule.getFactory().createTransitRoute(
				Id.create("route1", TransitRoute.class),
				null, // route will be set below
				java.util.Arrays.asList(routeStop1, routeStop2),
				"bus");

		// Create transit line
		TransitLine line = schedule.getFactory().createTransitLine(Id.create("line1", TransitLine.class));
		line.addRoute(route);
		schedule.addTransitLine(line);

		// Write to file
		String filename = tempDir.getAbsolutePath() + "/transitSchedule.xml";
		new TransitScheduleWriterV2(schedule).write(filename);

		// Read from file
		Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReaderV2(readScenario.getTransitSchedule(), new RouteFactories()).readFile(filename);

		// Verify that minimum stop durations are preserved
		TransitSchedule readSchedule = readScenario.getTransitSchedule();
		TransitLine readLine = readSchedule.getTransitLines().get(Id.create("line1", TransitLine.class));
		TransitRoute readRoute = readLine.getRoutes().get(Id.create("route1", TransitRoute.class));
		
		assertEquals(2, readRoute.getStops().size());
		assertEquals(30.0, readRoute.getStops().get(0).getMinimumStopDuration(), MatsimTestUtils.EPSILON);
		assertEquals(60.0, readRoute.getStops().get(1).getMinimumStopDuration(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testDefaultMinimumStopDuration() {
		// Test that default value is 0.0
		TransitStopFacility stop = new TransitStopFacilityImpl(Id.create("stop1", TransitStopFacility.class), new Coord(0, 0), false);
		TransitRouteStop routeStop = new TransitRouteStopImpl.Builder()
				.stop(stop)
				.arrivalOffset(0)
				.departureOffset(30)
				.build();
		
		assertEquals(0.0, routeStop.getMinimumStopDuration(), MatsimTestUtils.EPSILON);
	}
} 