/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTripDurationsTest.java
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

package playground.meisterk.org.matsim.analysis;

import org.matsim.core.events.Events;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test class for {@link CalctripDurations}.
 * 
 * @author meisterk
 *
 */
public class CalcTripDurationsTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testNoEvents() {
		
		CalcTripDurations testee = new CalcTripDurations();
		
		Events events = new Events();
		events.addHandler(testee);
		
		// add events to handle here
		
		this.runTest(testee);
	}
	
	protected void runTest(CalcTripDurations calcTripDurations) {
		
		calcTripDurations.writeStats(this.getOutputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);
		assertEquals("Output files differ.", expectedChecksum, actualChecksum);
	}

}
