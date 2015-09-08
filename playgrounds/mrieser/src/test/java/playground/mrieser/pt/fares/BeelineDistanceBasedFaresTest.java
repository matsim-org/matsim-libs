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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.testcases.MatsimTestCase;

public final class BeelineDistanceBasedFaresTest extends MatsimTestCase {

	public void testGetSingleTripCost_DifferentCostsPerKilometer() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl fromStop = facilities.createAndAddFacility(Id.create(1, ActivityFacility.class), new Coord((double) 100, (double) 200));
		final ActivityFacilityImpl toStop = facilities.createAndAddFacility(Id.create(2, ActivityFacility.class), new Coord((double) 2100, (double) 200));

		assertEquals(2.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(1.0, new BeelineDistanceBasedFares(0.5).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(3.0, new BeelineDistanceBasedFares(1.5).getSingleTripCost(fromStop, toStop), EPSILON);
	}

	public void testGetSingleTripCost_SameFromAsTo() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl fromStop = facilities.createAndAddFacility(Id.create(1, ActivityFacility.class), new Coord((double) 100, (double) 200));

		assertEquals(0.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, fromStop), EPSILON);
	}
}
