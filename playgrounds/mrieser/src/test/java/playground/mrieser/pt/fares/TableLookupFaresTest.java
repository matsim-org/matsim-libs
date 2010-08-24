/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneBasedFaresTest.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class TableLookupFaresTest extends MatsimTestCase {

	public void testGetSingleTripCost_SameFromAsTo() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final ActivityFacilityImpl stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final ActivityFacilityImpl stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double> fares = new HashMap<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double>();
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop1, stop2), 2.0);
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop2, stop3), 3.0);

		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop1, stop1), EPSILON);
		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop2), EPSILON);
		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop3, stop3), EPSILON);
	}

	public void testGetSingleTripCost_BasicQueries() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final ActivityFacilityImpl stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final ActivityFacilityImpl stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double> fares = new HashMap<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double>();
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop1, stop2), 2.0);
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop2, stop3), 3.0);

		assertEquals(2.0, new TableLookupFares(fares).getSingleTripCost(stop1, stop2), EPSILON);
		assertEquals(3.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop3), EPSILON);
		assertEquals(Double.NaN, new TableLookupFares(fares).getSingleTripCost(stop1, stop3), EPSILON);
	}

	public void testGetSingleTripCost_ReverseQueries() {
		// not clear if this is a feature or a bug...
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		final ActivityFacilityImpl stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final ActivityFacilityImpl stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final ActivityFacilityImpl stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double> fares = new HashMap<Tuple<ActivityFacilityImpl, ActivityFacilityImpl>, Double>();
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop1, stop2), 2.0);
		fares.put(new Tuple<ActivityFacilityImpl, ActivityFacilityImpl>(stop2, stop3), 3.0);

		assertEquals(2.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop1), EPSILON);
		assertEquals(3.0, new TableLookupFares(fares).getSingleTripCost(stop3, stop2), EPSILON);
		assertEquals(Double.NaN, new TableLookupFares(fares).getSingleTripCost(stop3, stop1), EPSILON);
	}
}
