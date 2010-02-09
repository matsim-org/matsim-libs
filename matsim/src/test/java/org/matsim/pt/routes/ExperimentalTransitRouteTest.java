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

import java.util.Collections;

import junit.framework.TestCase;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class ExperimentalTransitRouteTest extends TestCase {

	public void testInitializationLinks() {
		Link link1 = new FakeLink(new IdImpl(1));
		Link link2 = new FakeLink(new IdImpl(2));
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(link1.getId(), link2.getId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
		assertNull(route.getAccessStopId());
		assertNull(route.getLineId());
		assertNull(route.getEgressStopId());
	}

	public void testInitializationStops() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl(1), new CoordImpl(5, 11), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl(2), new CoordImpl(18, 7), false);
		Link link1 = new FakeLink(new IdImpl(3));
		Link link2 = new FakeLink(new IdImpl(4));
		stop1.setLinkId(link1.getId());
		stop2.setLinkId(link2.getId());
		TransitLine line = builder.createTransitLine(new IdImpl(5));
		TransitRoute tRoute = builder.createTransitRoute(new IdImpl(6), null, Collections.<TransitRouteStop>emptyList(), TransportMode.bus);
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(stop1, line, tRoute, stop2);
		assertEquals(stop1.getId(), route.getAccessStopId());
		assertEquals(line.getId(), route.getLineId());
		assertEquals(tRoute.getId(), route.getRouteId());
		assertEquals(stop2.getId(), route.getEgressStopId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
	}

	public void testLinks() {
		Link link1 = new FakeLink(new IdImpl(1));
		Link link2 = new FakeLink(new IdImpl(2));
		Link link3 = new FakeLink(new IdImpl(3));
		Link link4 = new FakeLink(new IdImpl(4));
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(link1.getId(), link2.getId());
		assertEquals(link1.getId(), route.getStartLinkId());
		assertEquals(link2.getId(), route.getEndLinkId());
		route.setStartLinkId(link3.getId());
		route.setEndLinkId(link4.getId());
		assertEquals(link3.getId(), route.getStartLinkId());
		assertEquals(link4.getId(), route.getEndLinkId());
	}

	public void testTravelTime() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		assertEquals(Time.UNDEFINED_TIME, route.getTravelTime(), MatsimTestCase.EPSILON);
		double traveltime = 987.65;
		route.setTravelTime(traveltime);
		assertEquals(traveltime, route.getTravelTime(), MatsimTestCase.EPSILON);
	}

	public void testDistance() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		assertTrue(Double.isNaN(route.getDistance()));
		double distance = 123.45;
		route.setDistance(distance);
		assertEquals(distance, route.getDistance(), MatsimTestCase.EPSILON);
	}

	public void testSetRouteDescription_PtRoute() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		route.setRouteDescription(null, "PT1===5===11===1980===1055", null);
		assertEquals("5", route.getAccessStopId().toString());
		assertEquals("11", route.getLineId().toString());
		assertEquals("1980", route.getRouteId().toString());
		assertEquals("1055", route.getEgressStopId().toString());
		assertEquals("PT1===5===11===1980===1055", route.getRouteDescription());
	}

	public void testSetRouteDescription_PtRouteWithDescription() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		route.setRouteDescription(null, "PT1===5===11===1980===1055===this is a===valid route", null);
		assertEquals("5", route.getAccessStopId().toString());
		assertEquals("11", route.getLineId().toString());
		assertEquals("1980", route.getRouteId().toString());
		assertEquals("1055", route.getEgressStopId().toString());
		assertEquals("PT1===5===11===1980===1055===this is a===valid route", route.getRouteDescription());
	}

	public void testSetRouteDescription_NonPtRoute() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		route.setRouteDescription(null, "23 42 7 21", null);
		assertNull(route.getAccessStopId());
		assertNull(route.getLineId());
		assertNull(route.getEgressStopId());
		assertEquals("23 42 7 21", route.getRouteDescription());
	}
}
