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
import org.junit.Test;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.PStrategy;

/**
 * @author droeder
 *
 */
public class RectangleHullRouteExtensionTest {

	@Test
    public final void testRun() {
		MatsimRandom.reset();
		PConfigGroup c = new PConfigGroup();
		Cooperative coop = PScenarioHelper.createCoop2414to3444();
		PPlan plan = coop.getBestPlan();
		//check the initial plan
		Assert.assertEquals(2, plan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c.getPIdentifier() + "2414", plan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals(c.getPIdentifier() + "3444", plan.getStopsToBeServed().get(1).getId().toString());
		// set up strategy
		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("0.1");
		PStrategy strategy = new RectangleHullRouteExtension(parameters);
		// run strategy with initial cooperative
		Assert.assertNull("new plan should be null", strategy.run(coop));
		// create new Plan
		coop.getBestPlan().setNVehicles(2);
		plan = strategy.run(coop);
		Assert.assertNotNull("new plan should not be null", plan);
		Assert.assertEquals(3, plan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c.getPIdentifier() + "2414", plan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals(c.getPIdentifier() + "1314", plan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals(c.getPIdentifier() + "3444", plan.getStopsToBeServed().get(2).getId().toString());
	}
}
