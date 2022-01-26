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

package org.matsim.contrib.minibus.replanning;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.minibus.hook.Operator;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;


public class MaxRandomStartTimeAllocatorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Operator coop = PScenarioHelper.createTestCooperative(utils.getOutputDirectory());
		ArrayList<String> param = new ArrayList<>();
		param.add("0");
		param.add("1");
		param.add("false");
		MaxRandomStartTimeAllocator strat = new MaxRandomStartTimeAllocator(param);
		PPlan testPlan = null;
		
		coop.getBestPlan().setStartTime(12000.0);
		coop.getBestPlan().setEndTime(36000.0);

		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 12000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// run strategy - time mutation is zero, thus no change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 12000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 12000.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		
		param = new ArrayList<>();
		param.add("900");
		param.add("10");
		param.add("false");
		strat = new MaxRandomStartTimeAllocator(param);
		
		// enough vehicles for testing
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 12000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 11920.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
	}
}
