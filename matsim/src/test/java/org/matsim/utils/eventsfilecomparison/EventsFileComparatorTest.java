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

import static org.junit.Assert.assertEquals;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result.*;

import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 * @author laemmel
 */
public class EventsFileComparatorTest extends MatsimTestCase {

	@org.junit.Test public void testRetCode0() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events5.xml.gz";
		assertEquals("return val = "  + FILES_ARE_EQUAL, FILES_ARE_EQUAL, EventsFileComparator.compare(f1, f2));

		assertEquals("return val = "  + FILES_ARE_EQUAL, FILES_ARE_EQUAL, EventsFileComparator.compare(f2, f1));
	}

	@org.junit.Test public void testRetCodeM1() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events1.xml.gz";
		assertEquals("return val " +DIFFERENT_NUMBER_OF_TIMESTEPS, DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f1, f2));

		assertEquals("return val " +DIFFERENT_NUMBER_OF_TIMESTEPS, DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f2, f1));
	}

	@org.junit.Test public void testRetCodeM2() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events2.xml.gz";
		assertEquals("return val = " + DIFFERENT_TIMESTEPS, DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f1, f2));

		assertEquals("return val = " + DIFFERENT_TIMESTEPS, DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f2, f1));
	}

	@org.junit.Test public void testRetCodeM3() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events3.xml.gz";
		assertEquals("return val = " + MISSING_EVENT, MISSING_EVENT, EventsFileComparator.compare(f1, f2));

		assertEquals("return val = " + MISSING_EVENT, MISSING_EVENT, EventsFileComparator.compare(f2, f1));
	}

	@org.junit.Test public void testRetCodeM4() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events4.xml.gz";
		assertEquals("return val = " + WRONG_EVENT_COUNT, WRONG_EVENT_COUNT, EventsFileComparator.compare(f1, f2));

		assertEquals("return val = " + WRONG_EVENT_COUNT, WRONG_EVENT_COUNT, EventsFileComparator.compare(f2, f1));
	}
}
