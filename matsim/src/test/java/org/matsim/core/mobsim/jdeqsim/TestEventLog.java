/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.mobsim.jdeqsim.util.CppEventFileParser;
import org.matsim.testcases.MatsimTestUtils;

public class TestEventLog {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test public void testGetTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile(utils.getPackageInputDirectory() + "deq_events.txt");
		assertEquals(3599.0, Math.floor(EventLog.getTravelTime(deqSimLog,1)), MatsimTestUtils.EPSILON);
	}

	@Test public void testGetAverageTravelTime(){
		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile(utils.getPackageInputDirectory() + "deq_events.txt");
		assertEquals(EventLog.getTravelTime(deqSimLog,1), EventLog.getSumTravelTime(deqSimLog), MatsimTestUtils.EPSILON);
	}
}
