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

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

public class AbstractFacilityAlgorithmTest extends MatsimTestCase {

	public void testRunAlgorithms() {
		final ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		// create 2 facilities
		facilities.createAndAddFacility(Id.create(1, ActivityFacility.class), new CoordImpl(1.0, 1.0));
		facilities.createAndAddFacility(Id.create(2, ActivityFacility.class), new CoordImpl(2.0, 2.0));
		// create an algo and let it run over the facilities
		MockAlgo1 algo1 = new MockAlgo1();
		algo1.run(facilities);
		assertEquals("TestAlgo should have handled 2 facilities.", 2, algo1.getCounter());
	}

	/*package*/ static class MockAlgo1 extends AbstractFacilityAlgorithm {
		private int counter = 0;

		@Override
		public void run(final ActivityFacility facility) {
			this.counter++;
		}

		public int getCounter() {
			return this.counter;
		}
	}
}
