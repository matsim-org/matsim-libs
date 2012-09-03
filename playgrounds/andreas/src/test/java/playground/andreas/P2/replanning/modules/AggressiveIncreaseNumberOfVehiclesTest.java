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

package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.AggressiveIncreaseNumberOfVehicles;


public class AggressiveIncreaseNumberOfVehiclesTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Cooperative coop = PScenarioHelper.createTestCooperative();
		AggressiveIncreaseNumberOfVehicles strat = new AggressiveIncreaseNumberOfVehicles(new ArrayList<String>());
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare budget", 0.0, coop.getBudget(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		// nothing should change, due to insufficient funds
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare budget", 0.0, coop.getBudget(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.setBudget(1000.0);
		// nothing should change, due to insufficient funds
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare budget", 1000.0, coop.getBudget(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.setBudget(1501.0);
		// enough funds to buy one vehicle
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare budget", 501.0, coop.getBudget(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		
		coop.setBudget(5501.0);
		// enough funds to buy five vehicles
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare budget", 501.0, coop.getBudget(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 5.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);		
	}
}