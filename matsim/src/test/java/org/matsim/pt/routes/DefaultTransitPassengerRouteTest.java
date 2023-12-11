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
}
