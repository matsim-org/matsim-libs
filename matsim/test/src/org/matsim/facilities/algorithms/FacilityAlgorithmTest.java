/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityAlgorithmTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities.algorithms;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.shared.Coord;

public class FacilityAlgorithmTest extends MatsimTestCase {

	public void testRunAlgorithms() {
		final Facilities facilities = new Facilities();
		// create 2 facilities
		facilities.createFacility(new IdImpl(1), new Coord(1.0, 1.0));
		facilities.createFacility(new IdImpl(2), new Coord(2.0, 2.0));
		// create an algo and let it run over the facilities
		MockAlgo algo = new MockAlgo();
		algo.run(facilities);
		assertEquals("TestAlgo should have handled 2 facilities.", 2, algo.getCounter());
	}
	
	/*package*/ static class MockAlgo extends FacilityAlgorithm {
		private int counter = 0;
		
		@Override
		public void run(final Facility facility) {
			this.counter++;
		}
		
		public int getCounter() {
			return this.counter;
		}
	}
}
