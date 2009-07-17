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

package playground.marcel.pt.integration;

import org.matsim.testcases.MatsimTestCase;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class ExperimentalTransitRouteTest extends MatsimTestCase {

	public void testSetRouteDescription_PtRoute() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		route.setRouteDescription(null, "PT1 5 11 1980", null);
		assertEquals("5", route.getAccessStopId().toString());
		assertEquals("11", route.getLineId().toString());
		assertEquals("1980", route.getEgressStopId().toString());
		assertEquals("PT1 5 11 1980", route.getRouteDescription());
	}

	public void testSetRouteDescription_PtRouteWithDescription() {
		ExperimentalTransitRoute route = new ExperimentalTransitRoute(null, null);
		route.setRouteDescription(null, "PT1 5 11 1980 this is a valid route", null);
		assertEquals("5", route.getAccessStopId().toString());
		assertEquals("11", route.getLineId().toString());
		assertEquals("1980", route.getEgressStopId().toString());
		assertEquals("PT1 5 11 1980 this is a valid route", route.getRouteDescription());
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
