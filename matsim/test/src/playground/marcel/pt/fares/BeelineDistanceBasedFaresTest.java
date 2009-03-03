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

package playground.marcel.pt.fares;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

public final class BeelineDistanceBasedFaresTest extends MatsimTestCase {

	public void testGetSingleTripCost_DifferentCostsPerKilometer() {
		final Facilities facilities = new FacilitiesImpl();
		final Facility fromStop = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final Facility toStop = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));

		assertEquals(2.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(1.0, new BeelineDistanceBasedFares(0.5).getSingleTripCost(fromStop, toStop), EPSILON);
		assertEquals(3.0, new BeelineDistanceBasedFares(1.5).getSingleTripCost(fromStop, toStop), EPSILON);
	}

	public void testGetSingleTripCost_SameFromAsTo() {
		final Facilities facilities = new FacilitiesImpl();
		final Facility fromStop = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));

		assertEquals(0.0, new BeelineDistanceBasedFares(1.0).getSingleTripCost(fromStop, fromStop), EPSILON);
	}
}
