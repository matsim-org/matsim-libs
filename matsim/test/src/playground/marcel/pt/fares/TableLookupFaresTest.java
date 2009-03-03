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

package playground.marcel.pt.fares;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.CoordImpl;

public class TableLookupFaresTest extends MatsimTestCase {

	public void testGetSingleTripCost_SameFromAsTo() {
		final Facilities facilities = new Facilities();
		final Facility stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final Facility stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final Facility stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<Facility, Facility>, Double> fares = new HashMap<Tuple<Facility, Facility>, Double>();
		fares.put(new Tuple<Facility, Facility>(stop1, stop2), 2.0);
		fares.put(new Tuple<Facility, Facility>(stop2, stop3), 3.0);

		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop1, stop1), EPSILON);
		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop2), EPSILON);
		assertEquals(0.0, new TableLookupFares(fares).getSingleTripCost(stop3, stop3), EPSILON);
	}

	public void testGetSingleTripCost_BasicQueries() {
		final Facilities facilities = new Facilities();
		final Facility stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final Facility stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final Facility stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<Facility, Facility>, Double> fares = new HashMap<Tuple<Facility, Facility>, Double>();
		fares.put(new Tuple<Facility, Facility>(stop1, stop2), 2.0);
		fares.put(new Tuple<Facility, Facility>(stop2, stop3), 3.0);

		assertEquals(2.0, new TableLookupFares(fares).getSingleTripCost(stop1, stop2), EPSILON);
		assertEquals(3.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop3), EPSILON);
		assertEquals(Double.NaN, new TableLookupFares(fares).getSingleTripCost(stop1, stop3), EPSILON);
	}

	public void testGetSingleTripCost_ReverseQueries() {
		// not clear if this is a feature or a bug...
		final Facilities facilities = new Facilities();
		final Facility stop1 = facilities.createFacility(new IdImpl(1), new CoordImpl(100, 200));
		final Facility stop2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2100, 200));
		final Facility stop3 = facilities.createFacility(new IdImpl(3), new CoordImpl(1100, 1200));

		final Map<Tuple<Facility, Facility>, Double> fares = new HashMap<Tuple<Facility, Facility>, Double>();
		fares.put(new Tuple<Facility, Facility>(stop1, stop2), 2.0);
		fares.put(new Tuple<Facility, Facility>(stop2, stop3), 3.0);

		assertEquals(2.0, new TableLookupFares(fares).getSingleTripCost(stop2, stop1), EPSILON);
		assertEquals(3.0, new TableLookupFares(fares).getSingleTripCost(stop3, stop2), EPSILON);
		assertEquals(Double.NaN, new TableLookupFares(fares).getSingleTripCost(stop3, stop1), EPSILON);
	}
}
