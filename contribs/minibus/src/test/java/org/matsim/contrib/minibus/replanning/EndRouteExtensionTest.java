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
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.replanning.EndRouteExtension;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;


public class EndRouteExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Operator coop = PScenarioHelper.createCoop2111to2333();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
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
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.5");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2333", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3343", testPlan.getStopsToBeServed().get(2).getId().toString());
		
		
		parameter = new ArrayList<>();
		parameter.add("2000.0");
		parameter.add("0.5");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2333", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3334", testPlan.getStopsToBeServed().get(2).getId().toString());
		
		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);

	}
	
	@Test
    public final void testRunVShapedRoute() {
	
		Operator coop = PScenarioHelper.createCoopRouteVShaped();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2111", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3222", coop.getBestPlan().getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServed().get(3).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_2223", testPlan.getStopsToBeServed().get(3).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(4).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1323", testPlan.getStopsToBeServed().get(3).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(4).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("1500.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1413", testPlan.getStopsToBeServed().get(3).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServed().get(4).getId().toString());
		
		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(testPlan.getLine());
		
		testPlan = strat.run(coop);
		
		// Adds stop 2414
		Assert.assertEquals("Compare new end stop", "p_2414", testPlan.getStopsToBeServed().get(4).getId().toString());
		
		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(testPlan.getLine());
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);
	}
}