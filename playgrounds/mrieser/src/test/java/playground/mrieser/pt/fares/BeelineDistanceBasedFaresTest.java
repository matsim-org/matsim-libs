/* *********************************************************************** *
 * project: org.matsim.*
 * BeelineDistanceBasedFaresTest.java
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

package playground.mrieser.pt.fares;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public final class BeelineDistanceBasedFaresTest extends MatsimTestCase {

	public void testGetSingleTripCost_DifferentCostsPerKilometer() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl fromStop = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final ActivityFacilityImpl toStop = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));

		assertEquals(2.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(1.0, new BeelineDistanceBasedFares(0.5).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(3.0, new BeelineDistanceBasedFares(1.5).getSingleTripCost(fromStop, toStop), EPSILON);
	}

	public void testGetSingleTripCost_SameFromAsTo() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl fromStop = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));

		assertEquals(0.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, fromStop), EPSILON);
	}
}
