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

import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 * @author laemmel
 */
public class EventsFileComparatorTest extends MatsimTestCase {

	public void testRetCode0() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events5.xml.gz";
		int i = EventsFileComparator.compareAndReturnInt(f1, f2);
		assertEquals("return val = 0", EventsFileComparator.CODE_FILES_ARE_EQUAL, i);

		i = EventsFileComparator.compareAndReturnInt(f2, f1);
		assertEquals("return val = 0", EventsFileComparator.CODE_FILES_ARE_EQUAL, i);
	}

	public void testRetCodeM1() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events1.xml.gz";
		int i = EventsFileComparator.compareAndReturnInt(f1, f2);
		assertEquals("return val = -1", EventsFileComparator.CODE_DIFFERENT_NUMBER_OF_TIMESTEPS, i);

		i = EventsFileComparator.compareAndReturnInt(f2, f1);
		assertEquals("return val = -1", EventsFileComparator.CODE_DIFFERENT_NUMBER_OF_TIMESTEPS, i);
	}

	public void testRetCodeM2() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events2.xml.gz";
		int i = EventsFileComparator.compareAndReturnInt(f1, f2);
		assertEquals("return val = -2", EventsFileComparator.CODE_DIFFERENT_TIMESTEPS, i);

		i = EventsFileComparator.compareAndReturnInt(f2, f1);
		assertEquals("return val = -2", EventsFileComparator.CODE_DIFFERENT_TIMESTEPS, i);
	}

	public void testRetCodeM3() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events3.xml.gz";
		int i = EventsFileComparator.compareAndReturnInt(f1, f2);
		assertEquals("return val = -3", EventsFileComparator.CODE_MISSING_EVENT, i);

		i = EventsFileComparator.compareAndReturnInt(f2, f1);
		assertEquals("return val = -3", EventsFileComparator.CODE_MISSING_EVENT, i);
	}

	public void testRetCodeM4() {
		String f1 = getClassInputDirectory() + "/events0.xml.gz";
		String f2 = getClassInputDirectory() + "/events4.xml.gz";
		int i = EventsFileComparator.compareAndReturnInt(f1, f2);
		assertEquals("return val = -4", EventsFileComparator.CODE_WRONG_EVENT_COUNT, i);

		i = EventsFileComparator.compareAndReturnInt(f2, f1);
		assertEquals("return val = -4", EventsFileComparator.CODE_WRONG_EVENT_COUNT, i);
	}
}
