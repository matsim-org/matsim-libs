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

import java.io.File;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConstants;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;


public class EndRouteExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Cooperative coop = PScenarioHelper.createCoop2111to2333();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<String>();
		parameter.add("1000.0");
		parameter.add("0.0");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2111", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2333", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<String>();
		parameter.add("1000.0");
		parameter.add("0.5");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_3343", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2333", testPlan.getStopsToBeServed().get(2).getId().toString());
		
		
		parameter = new ArrayList<String>();
		parameter.add("2000.0");
		parameter.add("0.5");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_3334", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2333", testPlan.getStopsToBeServed().get(2).getId().toString());
		
		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);

	}
}