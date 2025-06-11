/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalTransitRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.routes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.fakes.FakeLink;

// Mainly copied from ExperimentalTransitRouteTest
public class DefaultTransitPassengerRouteTest {

	@Test
	void testReadWrite_null() {
		DefaultTransitPassengerRoute routeA = new DefaultTransitPassengerRoute(null, null);
		String description = routeA.getRouteDescription();

		DefaultTransitPassengerRoute routeB = new DefaultTransitPassengerRoute(null, null);
		routeB.setRouteDescription(description);

		assertNull(routeB.getAccessStopId());
		assertNull(routeB.getEgressStopId());
		assertNull(routeB.getLineId());
		assertNull(routeB.getRouteId());
	}

	@Test
	void testReadWrite() {
		Id<TransitStopFacility> accessId = Id.create("access", TransitStopFacility.class);
		Id<TransitStopFacility> egressId = Id.create("egress", TransitStopFacility.class);
		Id<TransitLine> lineId = Id.create("line", TransitLine.class);
		Id<TransitRoute> routeId = Id.create("route", TransitRoute.class);

		DefaultTransitPassengerRoute routeA = new DefaultTransitPassengerRoute(null, null, accessId, egressId, lineId, routeId);
		String description = routeA.getRouteDescription();

		DefaultTransitPassengerRoute routeB = new DefaultTransitPassengerRoute(null, null);
		routeB.setRouteDescription(description);

		assertEquals(accessId, routeB.getAccessStopId());
		assertEquals(egressId, routeB.getEgressStopId());
		assertEquals(lineId, routeB.getLineId());
		assertEquals(routeId, routeB.getRouteId());
	}

	@Test
	void testInitializationLinks() {
		Link link1 = new FakeLink(Id.create(1, Link.class));
		Link link2 = new FakeLink(Id.create(2, Link.class));
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(link1.getId(), link2.getId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
		assertNull(route.getAccessStopId());
		assertNull(route.getLineId());
		assertNull(route.getEgressStopId());
	}

	@Test
	void testInitializationStops() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(5, 11), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(18, 7), false);
		Link link1 = new FakeLink(Id.create(3, Link.class));
		Link link2 = new FakeLink(Id.create(4, Link.class));
		stop1.setLinkId(link1.getId());
		stop2.setLinkId(link2.getId());
		TransitLine line = builder.createTransitLine(Id.create(5, TransitLine.class));
		TransitRoute tRoute = builder.createTransitRoute(Id.create(6, TransitRoute.class), null, Collections.<TransitRouteStop>emptyList(), "bus");
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(stop1, line, tRoute, stop2);
		route.setBoardingTime(123.0);
		assertEquals(stop1.getId(), route.getAccessStopId());
		assertEquals(line.getId(), route.getLineId());
		assertEquals(tRoute.getId(), route.getRouteId());
		assertEquals(stop2.getId(), route.getEgressStopId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
		assertEquals(123.0, route.getBoardingTime().seconds(), 1e-3);
	}

	@Test
	void testLinks() {
		Link link1 = new FakeLink(Id.create(1, Link.class));
		Link link2 = new FakeLink(Id.create(2, Link.class));
		Link link3 = new FakeLink(Id.create(3, Link.class));
		Link link4 = new FakeLink(Id.create(4, Link.class));
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(link1.getId(), link2.getId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
		route.setStartLinkId(link3.getId());
		route.setEndLinkId(link4.getId());
		assertEquals(link3.getId(), route.getStartLinkId());
		assertEquals(link4.getId(), route.getEndLinkId());
	}

	@Test
	void testTravelTime() {
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(null, null);
		assertTrue(route.getTravelTime().isUndefined());
		double traveltime = 987.65;
		route.setTravelTime(traveltime);
		assertEquals(traveltime, route.getTravelTime().seconds(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testSetRouteDescription_PtRoute() {
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(null, null);
		route.setRouteDescription("" +
				"{" +
					"\"accessFacilityId\" : \"5\"," +
					"\"egressFacilityId\" : \"1055\"," +
					"\"transitLineId\" : \"11\"," +
					"\"transitRouteId\" : \"1980\"" +
				"}"
		);
		assertEquals("5", route.getAccessStopId().toString());
		assertEquals("11", route.getLineId().toString());
		assertEquals("1980", route.getRouteId().toString());
		assertEquals("1055", route.getEgressStopId().toString());
		assertEquals("{\"transitRouteId\":\"1980\",\"boardingTime\":\"undefined\",\"transitLineId\":\"11\",\"accessFacilityId\":\"5\",\"egressFacilityId\":\"1055\"}", route.getRouteDescription());
	}

	@Test
	void testSetRouteDescription_NonPtRoute() {
		try {
			DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(null, null);
			route.setRouteDescription("23 42 7 21");
			fail();
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	void testChainedRoute_Serialize() {
		// Create the main route
		Id<TransitStopFacility> accessId1 = Id.create("access1", TransitStopFacility.class);
		Id<TransitStopFacility> egressId1 = Id.create("egress1", TransitStopFacility.class);
		Id<TransitLine> lineId1 = Id.create("line1", TransitLine.class);
		Id<TransitRoute> routeId1 = Id.create("route1", TransitRoute.class);
		DefaultTransitPassengerRoute mainRoute = new DefaultTransitPassengerRoute(null, null, accessId1, egressId1, lineId1, routeId1);
		mainRoute.setBoardingTime(100.0);

		// Create a chained route
		Id<TransitStopFacility> accessId2 = Id.create("access2", TransitStopFacility.class);
		Id<TransitStopFacility> egressId2 = Id.create("egress2", TransitStopFacility.class);
		Id<TransitLine> lineId2 = Id.create("line2", TransitLine.class);
		Id<TransitRoute> routeId2 = Id.create("route2", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute = new DefaultTransitPassengerRoute(null, null, accessId2, egressId2, lineId2, routeId2);
		chainedRoute.setBoardingTime(200.0);

		// Set the chained route
		mainRoute.chainedRoute = chainedRoute;

		// Serialize the route
		String description = mainRoute.getRouteDescription();

		// Create a new route from the description
		DefaultTransitPassengerRoute deserializedRoute = new DefaultTransitPassengerRoute(null, null);
		deserializedRoute.setRouteDescription(description);

		// Check main route properties
		assertEquals(accessId1, deserializedRoute.getAccessStopId());
		assertEquals(egressId1, deserializedRoute.getEgressStopId());
		assertEquals(lineId1, deserializedRoute.getLineId());
		assertEquals(routeId1, deserializedRoute.getRouteId());
		assertEquals(100.0, deserializedRoute.getBoardingTime().seconds(), 1e-3);

		// Check that chained route exists
		assertNotNull(deserializedRoute.getChainedRoute());

		// Check chained route properties
		DefaultTransitPassengerRoute deserializedChainedRoute = deserializedRoute.getChainedRoute();
		assertEquals(accessId2, deserializedChainedRoute.getAccessStopId());
		assertEquals(egressId2, deserializedChainedRoute.getEgressStopId());
		assertEquals(lineId2, deserializedChainedRoute.getLineId());
		assertEquals(routeId2, deserializedChainedRoute.getRouteId());
		assertEquals(200.0, deserializedChainedRoute.getBoardingTime().seconds(), 1e-3);
	}

	@Test
	void testNestedChainedRoutes() {
		// Create the main route
		Id<TransitStopFacility> accessId1 = Id.create("access1", TransitStopFacility.class);
		Id<TransitStopFacility> egressId1 = Id.create("egress1", TransitStopFacility.class);
		Id<TransitLine> lineId1 = Id.create("line1", TransitLine.class);
		Id<TransitRoute> routeId1 = Id.create("route1", TransitRoute.class);
		DefaultTransitPassengerRoute mainRoute = new DefaultTransitPassengerRoute(null, null, accessId1, egressId1, lineId1, routeId1);

		// Create first chained route
		Id<TransitStopFacility> accessId2 = Id.create("access2", TransitStopFacility.class);
		Id<TransitStopFacility> egressId2 = Id.create("egress2", TransitStopFacility.class);
		Id<TransitLine> lineId2 = Id.create("line2", TransitLine.class);
		Id<TransitRoute> routeId2 = Id.create("route2", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute1 = new DefaultTransitPassengerRoute(null, null, accessId2, egressId2, lineId2, routeId2);

		// Create second chained route
		Id<TransitStopFacility> accessId3 = Id.create("access3", TransitStopFacility.class);
		Id<TransitStopFacility> egressId3 = Id.create("egress3", TransitStopFacility.class);
		Id<TransitLine> lineId3 = Id.create("line3", TransitLine.class);
		Id<TransitRoute> routeId3 = Id.create("route3", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute2 = new DefaultTransitPassengerRoute(null, null, accessId3, egressId3, lineId3, routeId3);

		// Link the routes
		mainRoute.chainedRoute = chainedRoute1;
		chainedRoute1.chainedRoute = chainedRoute2;

		// Serialize the route
		String description = mainRoute.getRouteDescription();

		// Create a new route from the description
		DefaultTransitPassengerRoute deserializedRoute = new DefaultTransitPassengerRoute(null, null);
		deserializedRoute.setRouteDescription(description);

		// Check first level
		assertEquals(accessId1, deserializedRoute.getAccessStopId());
		assertEquals(lineId1, deserializedRoute.getLineId());

		// Check second level
		DefaultTransitPassengerRoute deserializedChainedRoute1 = deserializedRoute.getChainedRoute();
		assertNotNull(deserializedChainedRoute1);
		assertEquals(accessId2, deserializedChainedRoute1.getAccessStopId());
		assertEquals(lineId2, deserializedChainedRoute1.getLineId());

		// Check third level
		DefaultTransitPassengerRoute deserializedChainedRoute2 = deserializedChainedRoute1.getChainedRoute();
		assertNotNull(deserializedChainedRoute2);
		assertEquals(accessId3, deserializedChainedRoute2.getAccessStopId());
		assertEquals(lineId3, deserializedChainedRoute2.getLineId());

		// Check that there's no fourth level
		assertNull(deserializedChainedRoute2.getChainedRoute());
	}

	@Test
	void testClone_WithChainedRoute() {
		// Create the main route
		Id<TransitStopFacility> accessId1 = Id.create("access1", TransitStopFacility.class);
		Id<TransitStopFacility> egressId1 = Id.create("egress1", TransitStopFacility.class);
		Id<TransitLine> lineId1 = Id.create("line1", TransitLine.class);
		Id<TransitRoute> routeId1 = Id.create("route1", TransitRoute.class);
		DefaultTransitPassengerRoute mainRoute = new DefaultTransitPassengerRoute(null, null, accessId1, egressId1, lineId1, routeId1);
		mainRoute.setBoardingTime(100.0);

		// Create a chained route
		Id<TransitStopFacility> accessId2 = Id.create("access2", TransitStopFacility.class);
		Id<TransitStopFacility> egressId2 = Id.create("egress2", TransitStopFacility.class);
		Id<TransitLine> lineId2 = Id.create("line2", TransitLine.class);
		Id<TransitRoute> routeId2 = Id.create("route2", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute = new DefaultTransitPassengerRoute(null, null, accessId2, egressId2, lineId2, routeId2);
		chainedRoute.setBoardingTime(200.0);

		// Set the chained route
		mainRoute.chainedRoute = chainedRoute;

		// Clone the route
		DefaultTransitPassengerRoute clonedRoute = mainRoute.clone();

		// Check main route properties
		assertEquals(accessId1, clonedRoute.getAccessStopId());
		assertEquals(egressId1, clonedRoute.getEgressStopId());
		assertEquals(lineId1, clonedRoute.getLineId());
		assertEquals(routeId1, clonedRoute.getRouteId());
		assertEquals(100.0, clonedRoute.getBoardingTime().seconds(), 1e-3);

		// Check that chained route exists and is a different instance (deep copy)
		assertNotNull(clonedRoute.getChainedRoute());
		assertNotSame(chainedRoute, clonedRoute.getChainedRoute());

		// Verify properties of the cloned chained route
		DefaultTransitPassengerRoute clonedChainedRoute = clonedRoute.getChainedRoute();
		assertEquals(accessId2, clonedChainedRoute.getAccessStopId());
		assertEquals(egressId2, clonedChainedRoute.getEgressStopId());
		assertEquals(lineId2, clonedChainedRoute.getLineId());
		assertEquals(routeId2, clonedChainedRoute.getRouteId());
		assertEquals(200.0, clonedChainedRoute.getBoardingTime().seconds(), 1e-3);
	}

	@Test
	void testClone_WithNestedChainedRoutes() {
		// Create the main route
		Id<TransitStopFacility> accessId1 = Id.create("access1", TransitStopFacility.class);
		Id<TransitStopFacility> egressId1 = Id.create("egress1", TransitStopFacility.class);
		Id<TransitLine> lineId1 = Id.create("line1", TransitLine.class);
		Id<TransitRoute> routeId1 = Id.create("route1", TransitRoute.class);
		DefaultTransitPassengerRoute mainRoute = new DefaultTransitPassengerRoute(null, null, accessId1, egressId1, lineId1, routeId1);
		mainRoute.setTravelTime(100.0);

		// Create first chained route
		Id<TransitStopFacility> accessId2 = Id.create("access2", TransitStopFacility.class);
		Id<TransitStopFacility> egressId2 = Id.create("egress2", TransitStopFacility.class);
		Id<TransitLine> lineId2 = Id.create("line2", TransitLine.class);
		Id<TransitRoute> routeId2 = Id.create("route2", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute1 = new DefaultTransitPassengerRoute(null, null, accessId2, egressId2, lineId2, routeId2);
		chainedRoute1.setTravelTime(200.0);

		// Create second chained route
		Id<TransitStopFacility> accessId3 = Id.create("access3", TransitStopFacility.class);
		Id<TransitStopFacility> egressId3 = Id.create("egress3", TransitStopFacility.class);
		Id<TransitLine> lineId3 = Id.create("line3", TransitLine.class);
		Id<TransitRoute> routeId3 = Id.create("route3", TransitRoute.class);
		DefaultTransitPassengerRoute chainedRoute2 = new DefaultTransitPassengerRoute(null, null, accessId3, egressId3, lineId3, routeId3);

		// Link the routes
		mainRoute.chainedRoute = chainedRoute1;
		chainedRoute1.chainedRoute = chainedRoute2;

		// Clone the route
		DefaultTransitPassengerRoute clonedRoute = mainRoute.clone();

		// Check first level is a different instance
		assertNotSame(mainRoute, clonedRoute);
		assertEquals(accessId1, clonedRoute.getAccessStopId());

		// Check second level is a different instance
		DefaultTransitPassengerRoute clonedChainedRoute1 = clonedRoute.getChainedRoute();
		assertNotNull(clonedChainedRoute1);
		assertNotSame(chainedRoute1, clonedChainedRoute1);
		assertEquals(accessId2, clonedChainedRoute1.getAccessStopId());

		// Check third level is a different instance
		DefaultTransitPassengerRoute clonedChainedRoute2 = clonedChainedRoute1.getChainedRoute();
		assertNotNull(clonedChainedRoute2);
		assertNotSame(chainedRoute2, clonedChainedRoute2);
		assertEquals(accessId3, clonedChainedRoute2.getAccessStopId());

		// Check that modifying the original doesn't affect the clone
		chainedRoute1.setTravelTime(999.0);
		assertNotEquals(999.0, clonedChainedRoute1.getTravelTime().seconds(), 1e-3);
	}

}
