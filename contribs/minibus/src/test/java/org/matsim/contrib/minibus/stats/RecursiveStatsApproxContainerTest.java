/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.minibus.stats.RecursiveStatsApproxContainer;
import org.matsim.testcases.MatsimTestUtils;


public class RecursiveStatsApproxContainerTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testRecursiveStatsContainer() {

		RecursiveStatsApproxContainer stats = new RecursiveStatsApproxContainer(0.1, 3);

		stats.handleNewEntry(1.0, 4.0, 2.0, 3.0);
		Assert.assertEquals("mean coop", 1.0, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean route", 4.0, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean pax", 2.0, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean veh", 3.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev coop", Double.NaN, stats.getStdDevOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev route", Double.NaN, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev pax", Double.NaN, stats.getStdDevPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev veh", Double.NaN, stats.getStdDevVeh(), MatsimTestUtils.EPSILON);
		
		stats.handleNewEntry(2.0, 3.0, 3.0, 1.0);
		Assert.assertEquals("mean coop", 1.5, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean route", 3.5, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean pax", 2.5, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean veh", 2.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev coop", 0.7071067811865476, stats.getStdDevOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev route", 0.7071067811865476, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev pax", 0.7071067811865476, stats.getStdDevPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev veh", 1.4142135623730951, stats.getStdDevVeh(), MatsimTestUtils.EPSILON);
		
		stats.handleNewEntry(3.0, 2.0, 1.0, 2.0);
		Assert.assertEquals("mean coop", 2.0, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean route", 3.0, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean pax", 2.0, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean veh", 2.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev coop", 1.0, stats.getStdDevOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev route", 1.0, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev pax", 1.0, stats.getStdDevPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev veh", 1.0, stats.getStdDevVeh(), MatsimTestUtils.EPSILON);
		
		stats.handleNewEntry(1.0, 4.0, 2.0, 3.0);
		stats.handleNewEntry(2.0, 3.0, 3.0, 1.0);
		stats.handleNewEntry(12.0, 12345.0, 123.0, 1234.0);
		Assert.assertEquals("mean coop", 2.9190000000000005, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean route", 1237.281, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean pax", 14.190000000000001, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean veh", 125.191, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev coop", 1.7181000000000002, stats.getStdDevOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev route", 1111.5819000000001, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev pax", 11.691, stats.getStdDevPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev veh", 111.7719, stats.getStdDevVeh(), MatsimTestUtils.EPSILON);
	}
}