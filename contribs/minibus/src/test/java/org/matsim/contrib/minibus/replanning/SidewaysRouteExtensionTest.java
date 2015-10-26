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
import org.matsim.contrib.minibus.replanning.SidewaysRouteExtension;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;


public class SidewaysRouteExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Operator coop = PScenarioHelper.createCoop2414to3444();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("0.0");
		parameter.add("0.0");
		parameter.add("true");
		
		SidewaysRouteExtension strat = new SidewaysRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2414", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.0");
		parameter.add("true");
		
		strat = new SidewaysRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		// enough buffer to add a stop located directly at the beeline
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2324", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2324", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		
		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.5");
		parameter.add("true");
		
		strat = new SidewaysRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		// enough buffer 0.5 * 3000m = 1500m 
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2223", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare start stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2223", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(coop.getRouteProvider().createTransitLineFromOperatorPlan(coop.getId(), testPlan));
		
		testPlan = strat.run(coop);
		
		// and again stacking - therefore, enlarging the effective buffer
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2212", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2223", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(3).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2212", testPlan.getStopsToBeServed().get(4).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2223", testPlan.getStopsToBeServed().get(5).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("4000.0");
		parameter.add("0.5");
		parameter.add("true");
		
		strat = new SidewaysRouteExtension(parameter);
		coop = PScenarioHelper.createCoop2414to3444();
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2324", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2324", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2223", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2223", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2223", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2223", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_3323", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3323", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_3433", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3433", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2423", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2423", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2322", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2322", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// quite a lot buffer covering all nodes
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2221", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2221", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.0");
		parameter.add("false");
		
		strat = new SidewaysRouteExtension(parameter);
		coop = PScenarioHelper.createCoop2414to3444();
		
		testPlan = strat.run(coop);
		
		// can now choose among stops at the outer edges
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2324", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2324", testPlan.getStopsToBeServed().get(3).getId().toString());
		
		testPlan = strat.run(coop);
		
		// can now choose among stops at the outer edges
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2414", testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Compare start stop", "p_B", testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3444", testPlan.getStopsToBeServed().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_B", testPlan.getStopsToBeServed().get(3).getId().toString());

	}
}