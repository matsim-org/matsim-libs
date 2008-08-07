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

package org.matsim.facilities;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.algorithms.AbstractFacilityAlgorithm;
import org.matsim.facilities.algorithms.FacilityAlgorithm;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * Tests several aspects of Facilities.
 *
 * @author mrieser
 */
public class FacilitiesTest extends MatsimTestCase {

	public void testAlgorithms() {
		final Facilities facilities = new Facilities();
		// create 3 facilities
		facilities.createFacility(new IdImpl(1), new CoordImpl(1.0, 1.0));
		facilities.createFacility(new IdImpl(2), new CoordImpl(2.0, 2.0));
		facilities.createFacility(new IdImpl(3), new CoordImpl(3.0, 3.0));
		// create 2 algo and add them to the facilities-object
		MockAlgo1 algo1 = new MockAlgo1();
		MockAlgo2 algo2 = new MockAlgo2();
		facilities.addAlgorithm(algo1);
		facilities.addAlgorithm(algo2);
		// run the algorithms a first time, each should get 3 facilities
		facilities.runAlgorithms();
		assertEquals("TestAlgo should have handled 3 facilities.", 3, algo1.getCounter());
		assertEquals("TestAlgo should have handled 3 facilities.", 3, algo2.getCounter());
		// run the algorithms again, they continue counting
		facilities.runAlgorithms();
		assertEquals("TestAlgo should have handled 6 facilities.", 6, algo1.getCounter());
		assertEquals("TestAlgo should have handled 6 facilities.", 6, algo2.getCounter());
		// clear algorithms and run again, they shouldn't count
		facilities.clearAlgorithms();
		facilities.runAlgorithms();
		assertEquals("TestAlgo should have handled 6 facilities.", 6, algo1.getCounter());
		assertEquals("TestAlgo should have handled 6 facilities.", 6, algo2.getCounter());
	}

	public void testStreaming() {
		final Facilities facilities = new Facilities("test", true);
		// create 2 algo and add them to the facilities-object
		MockAlgo1 algo1 = new MockAlgo1();
		MockAlgo2 algo2 = new MockAlgo2();
		facilities.addAlgorithm(algo1);
		facilities.addAlgorithm(algo2);
		// create a first facility
		Facility f1 = facilities.createFacility(new IdImpl(1), new CoordImpl(1.0, 1.0));
		facilities.finishFacility(f1);
		assertEquals(1, algo1.getCounter());
		assertEquals(1, algo2.getCounter());
		assertEquals("in streaming, facilities should contain no facility", 0, facilities.getFacilities().size());
		// create two other facilities
		Facility f2 = facilities.createFacility(new IdImpl(2), new CoordImpl(2.0, 2.0));
		facilities.finishFacility(f2);
		Facility f3 = facilities.createFacility(new IdImpl(3), new CoordImpl(3.0, 3.0));
		facilities.finishFacility(f3);
		assertEquals(3, algo1.getCounter());
		assertEquals(3, algo2.getCounter());
		assertEquals("in streaming, facilities should contain no facility", 0, facilities.getFacilities().size());
	}

	/*package*/ static class MockAlgo1 extends AbstractFacilityAlgorithm {
		private int counter = 0;

		public void run(final Facility facility) {
			this.counter++;
		}

		public int getCounter() {
			return this.counter;
		}
	}

	/*package*/ static class MockAlgo2 implements FacilityAlgorithm {
		private int counter = 0;

		public void run(final Facility facility) {
			this.counter++;
		}

		public int getCounter() {
			return this.counter;
		}
	}
}
