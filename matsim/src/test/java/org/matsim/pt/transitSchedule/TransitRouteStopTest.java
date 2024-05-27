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

package org.matsim.pt.transitSchedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class TransitRouteStopTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	protected TransitRouteStop createTransitRouteStop(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		return new TransitRouteStopImpl.Builder().stop(stop).arrivalOffset(arrivalDelay).departureOffset(departureDelay).build();
	}

	@Test
	void testInitialization() {
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		double arrivalDelay = 4;
		double departureDelay = 5;
		TransitRouteStop routeStop = createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
		assertEquals(stopFacility, routeStop.getStopFacility());
		assertEquals(arrivalDelay, routeStop.getArrivalOffset().seconds(), MatsimTestUtils.EPSILON);
		assertEquals(departureDelay, routeStop.getDepartureOffset().seconds(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testStopFacility() {
		TransitStopFacility stopFacility1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		TransitStopFacility stopFacility2 = new TransitStopFacilityImpl(Id.create(2, TransitStopFacility.class), new Coord((double) 3, (double) 4), false);
		double arrivalDelay = 4;
		double departureDelay = 5;
		TransitRouteStop routeStop = createTransitRouteStop(stopFacility1, arrivalDelay, departureDelay);
		assertEquals(stopFacility1, routeStop.getStopFacility());
		routeStop.setStopFacility(stopFacility2);
		assertEquals(stopFacility2, routeStop.getStopFacility());
	}

	@Test
	void testAwaitDepartureTime() {
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		double arrivalDelay = 4;
		double departureDelay = 5;
		TransitRouteStop routeStop = createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
		assertFalse(routeStop.isAwaitDepartureTime());
		routeStop.setAwaitDepartureTime(true);
		assertTrue(routeStop.isAwaitDepartureTime());
		routeStop.setAwaitDepartureTime(false);
		assertFalse(routeStop.isAwaitDepartureTime());
	}

	@Test
	void testEquals() {
		TransitStopFacility stopFacility1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		TransitStopFacility stopFacility2 = new TransitStopFacilityImpl(Id.create(2, TransitStopFacility.class), new Coord((double) 3, (double) 4), false);
		TransitRouteStop stop1 = createTransitRouteStop(stopFacility1, 10, 50);
		TransitRouteStop stop2 = createTransitRouteStop(stopFacility1, 10, 50);
		TransitRouteStop stop3 = createTransitRouteStop(stopFacility2, 10, 50);
		TransitRouteStop stop4 = createTransitRouteStop(stopFacility1, 10, 30);
		TransitRouteStop stop5 = createTransitRouteStop(stopFacility1, 20, 50);
		TransitRouteStop stop6 = createTransitRouteStop(null, 10, 50);
		TransitRouteStop stop7 = createTransitRouteStop(null, 10, 50);

		assertTrue(stop1.equals(stop2));
		assertTrue(stop2.equals(stop1));
		assertTrue(stop1.equals(stop1));

		assertFalse(stop1.equals(stop3)); // different stop facility
		assertFalse(stop3.equals(stop1));
		assertFalse(stop1.equals(stop4)); // different departureDelay
		assertFalse(stop4.equals(stop1));
		assertFalse(stop1.equals(stop5)); // different arrivalDelay
		assertFalse(stop5.equals(stop1));

		assertFalse(stop1.equals(stop6)); // null stop facility in stop6
		assertFalse(stop6.equals(stop1));

		assertTrue(stop6.equals(stop7)); // both stop facilities are null
		assertTrue(stop7.equals(stop6));
	}

}
