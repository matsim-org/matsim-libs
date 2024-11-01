/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.utils.eventsfilecomparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.utils.eventsfilecomparison.ComparisonResult.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 * @author laemmel
 */
public class EventsFileComparatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testRetCode0() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events5.xml.gz";
		assertEquals(FILES_ARE_EQUAL, EventsFileComparator.compare(f1, f2), "return val = "  + FILES_ARE_EQUAL);

		assertEquals(FILES_ARE_EQUAL, EventsFileComparator.compare(f2, f1), "return val = "  + FILES_ARE_EQUAL);
	}

	@Test
	void testRetCodeM1() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events1.xml.gz";
		assertEquals(DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f1, f2), "return val " +DIFFERENT_NUMBER_OF_TIMESTEPS);

		assertEquals(DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f2, f1), "return val " +DIFFERENT_NUMBER_OF_TIMESTEPS);
	}

	@Test
	void testRetCodeM2() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events2.xml.gz";
		assertEquals(DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f1, f2), "return val = " + DIFFERENT_TIMESTEPS);

		assertEquals(DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f2, f1), "return val = " + DIFFERENT_TIMESTEPS);
	}

	@Test
	void testRetCodeM3() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events3.xml.gz";
		assertEquals(MISSING_EVENT, EventsFileComparator.compare(f1, f2), "return val = " + MISSING_EVENT);

		assertEquals(MISSING_EVENT, EventsFileComparator.compare(f2, f1), "return val = " + MISSING_EVENT);
	}

	@Test
	void testRetCodeM4() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events4.xml.gz";
		assertEquals(WRONG_EVENT_COUNT, EventsFileComparator.compare(f1, f2), "return val = " + WRONG_EVENT_COUNT);

		assertEquals(WRONG_EVENT_COUNT, EventsFileComparator.compare(f2, f1), "return val = " + WRONG_EVENT_COUNT);
	}
}
