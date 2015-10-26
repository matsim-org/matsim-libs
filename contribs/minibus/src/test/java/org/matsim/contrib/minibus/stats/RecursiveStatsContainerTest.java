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
import org.matsim.contrib.minibus.stats.RecursiveStatsContainer;
import org.matsim.core.utils.misc.MatsimTestUtils;


public class RecursiveStatsContainerTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testRecursiveStatsContainer() {

		RecursiveStatsContainer stats = new RecursiveStatsContainer();

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
		Assert.assertEquals("mean coop", 3.5, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean route", 2060.1666666666665, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean pax", 22.33333333333, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("mean veh", 207.33333333333, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev coop", 4.230839160261236, stats.getStdDevOperators(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev route", 5038.518806818793, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev pax", 49.32207078648124, stats.getStdDevPax(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("std dev veh", 502.962689139728, stats.getStdDevVeh(), MatsimTestUtils.EPSILON);
	}
}