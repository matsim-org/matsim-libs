/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteStopTest.java
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

package org.matsim.transitSchedule;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class TransitRouteStopTest extends MatsimTestCase {

	protected TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		return new TransitRouteStopImpl(stop, arrivalDelay, departureDelay);
	}

	public void testInitialization() {
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(new IdImpl(1), new CoordImpl(2, 3), false);
		double arrivalDelay = 4;
		double departureDelay = 5;
		TransitRouteStop routeStop = createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
		assertEquals(stopFacility, routeStop.getStopFacility());
		assertEquals(arrivalDelay, routeStop.getArrivalOffset(), EPSILON);
		assertEquals(departureDelay, routeStop.getDepartureOffset(), EPSILON);
	}

	public void testAwaitDepartureTime() {
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(new IdImpl(1), new CoordImpl(2, 3), false);
		double arrivalDelay = 4;
		double departureDelay = 5;
		TransitRouteStop routeStop = createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
		assertFalse(routeStop.isAwaitDepartureTime());
		routeStop.setAwaitDepartureTime(true);
		assertTrue(routeStop.isAwaitDepartureTime());
		routeStop.setAwaitDepartureTime(false);
		assertFalse(routeStop.isAwaitDepartureTime());
	}

}
