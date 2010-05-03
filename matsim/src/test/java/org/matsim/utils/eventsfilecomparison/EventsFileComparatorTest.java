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

public class EventsFileComparatorTest  extends MatsimTestCase{

	public void testRetCode0() {
		String f1 = getInputDirectory().replace("/testRetCode0/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCode0/", "") + "/events5.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",0,i);

		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",0,i);
	}

	public void testRetCodeM1() {
		String f1 = getInputDirectory().replace("/testRetCodeM1/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM1/", "") + "/events1.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = -1",-1,i);

		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = -1",-1,i);
	}

	public void testRetCodeM2() {
		String f1 = getInputDirectory().replace("/testRetCodeM2/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM2/", "") + "/events2.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = -2",-2,i);

		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = -2",-2,i);
	}

	public void testRetCodeM3() {
		String f1 = getInputDirectory().replace("/testRetCodeM3/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM3/", "") + "/events3.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = -3",-3,i);

		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = -3",-3,i);
	}

	public void testRetCodeM4() {
		String f1 = getInputDirectory().replace("/testRetCodeM4/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM4/", "") + "/events4.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = -4",-4,i);

		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = -4",-4,i);
	}
}
