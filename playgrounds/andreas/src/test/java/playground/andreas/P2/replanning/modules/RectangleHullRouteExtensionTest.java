/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;

/**
 * @author droeder
 *
 */
public class RectangleHullRouteExtensionTest {

	@Test
    public final void testRun() {
		MatsimRandom.reset();
		Cooperative coop = PScenarioHelper.createCoop2414to3444();
		PPlan oldPlan = coop.getBestPlan();

		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("0.1");
		PPlanStrategy strategy = new RectangleHullRouteExtension(parameters);
		coop.init(coop.getRouteProvider(), strategy, 1);
		PPlan newPlan = coop.getBestPlan();
		
		Assert.assertEquals(2, oldPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("p_2414", oldPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("p_3444", oldPlan.getStopsToBeServed().get(1).getId().toString());
		
		Assert.assertEquals(3, newPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("p_2414", newPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("p_1314", newPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("p_3444", newPlan.getStopsToBeServed().get(2).getId().toString());
	}

}
