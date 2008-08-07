/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.population;

import org.matsim.population.Act;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;


/**
 * Test for convenience methods of Act.
 * @author dgrether
 *
 */
public class ActTest extends MatsimTestCase {

	private Act testee;
	
	/**
	 * @see org.matsim.testcases.MatsimTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		testee = new Act("h", 0.0, 0.0, null, 0.0, 0.0, 0.0, false);
		
	}
	
	
	public void testCalculateDuration() {
		assertNotNull(testee);
		assertEquals(0.0, testee.calculateDuration());
		testee.setEndTime(5.5 * 3600.0);
		assertEquals(5.5 * 3600.0, testee.calculateDuration());
		testee.setStartTime(Time.UNDEFINED_TIME);
		assertEquals(5.5 * 3600.0, testee.calculateDuration());
		testee.setEndTime(Time.UNDEFINED_TIME);
		assertEquals(0.0, testee.calculateDuration());
		testee.setDur(Time.UNDEFINED_TIME);
		Exception e = null;
		try {
			testee.calculateDuration();
		} catch (RuntimeException ex) {
			e = ex;
		}
		assertNotNull(e);
		testee.setStartTime(17 * 3600.0);
		assertEquals(7 * 3600.0, testee.calculateDuration());
	}
	
	

}
