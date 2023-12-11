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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.minibus.stats.RecursiveStatsApproxContainer;
import org.matsim.testcases.MatsimTestUtils;


public class RecursiveStatsApproxContainerTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testRecursiveStatsContainer() {

		RecursiveStatsApproxContainer stats = new RecursiveStatsApproxContainer(0.1, 3);

		stats.handleNewEntry(1.0, 4.0, 2.0, 3.0);
		Assertions.assertEquals(1.0, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON, "mean coop");
		Assertions.assertEquals(4.0, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON, "mean route");
		Assertions.assertEquals(2.0, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON, "mean pax");
		Assertions.assertEquals(3.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON, "mean veh");
		Assertions.assertEquals(Double.NaN, stats.getStdDevOperators(), MatsimTestUtils.EPSILON, "std dev coop");
		Assertions.assertEquals(Double.NaN, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON, "std dev route");
		Assertions.assertEquals(Double.NaN, stats.getStdDevPax(), MatsimTestUtils.EPSILON, "std dev pax");
		Assertions.assertEquals(Double.NaN, stats.getStdDevVeh(), MatsimTestUtils.EPSILON, "std dev veh");

		stats.handleNewEntry(2.0, 3.0, 3.0, 1.0);
		Assertions.assertEquals(1.5, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON, "mean coop");
		Assertions.assertEquals(3.5, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON, "mean route");
		Assertions.assertEquals(2.5, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON, "mean pax");
		Assertions.assertEquals(2.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON, "mean veh");
		Assertions.assertEquals(0.7071067811865476, stats.getStdDevOperators(), MatsimTestUtils.EPSILON, "std dev coop");
		Assertions.assertEquals(0.7071067811865476, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON, "std dev route");
		Assertions.assertEquals(0.7071067811865476, stats.getStdDevPax(), MatsimTestUtils.EPSILON, "std dev pax");
		Assertions.assertEquals(1.4142135623730951, stats.getStdDevVeh(), MatsimTestUtils.EPSILON, "std dev veh");

		stats.handleNewEntry(3.0, 2.0, 1.0, 2.0);
		Assertions.assertEquals(2.0, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON, "mean coop");
		Assertions.assertEquals(3.0, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON, "mean route");
		Assertions.assertEquals(2.0, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON, "mean pax");
		Assertions.assertEquals(2.0, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON, "mean veh");
		Assertions.assertEquals(1.0, stats.getStdDevOperators(), MatsimTestUtils.EPSILON, "std dev coop");
		Assertions.assertEquals(1.0, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON, "std dev route");
		Assertions.assertEquals(1.0, stats.getStdDevPax(), MatsimTestUtils.EPSILON, "std dev pax");
		Assertions.assertEquals(1.0, stats.getStdDevVeh(), MatsimTestUtils.EPSILON, "std dev veh");

		stats.handleNewEntry(1.0, 4.0, 2.0, 3.0);
		stats.handleNewEntry(2.0, 3.0, 3.0, 1.0);
		stats.handleNewEntry(12.0, 12345.0, 123.0, 1234.0);
		Assertions.assertEquals(2.9190000000000005, stats.getArithmeticMeanOperators(), MatsimTestUtils.EPSILON, "mean coop");
		Assertions.assertEquals(1237.281, stats.getArithmeticMeanRoutes(), MatsimTestUtils.EPSILON, "mean route");
		Assertions.assertEquals(14.190000000000001, stats.getArithmeticMeanPax(), MatsimTestUtils.EPSILON, "mean pax");
		Assertions.assertEquals(125.191, stats.getArithmeticMeanVeh(), MatsimTestUtils.EPSILON, "mean veh");
		Assertions.assertEquals(1.7181000000000002, stats.getStdDevOperators(), MatsimTestUtils.EPSILON, "std dev coop");
		Assertions.assertEquals(1111.5819000000001, stats.getStdDevRoutes(), MatsimTestUtils.EPSILON, "std dev route");
		Assertions.assertEquals(11.691, stats.getStdDevPax(), MatsimTestUtils.EPSILON, "std dev pax");
		Assertions.assertEquals(111.7719, stats.getStdDevVeh(), MatsimTestUtils.EPSILON, "std dev veh");
	}
}
