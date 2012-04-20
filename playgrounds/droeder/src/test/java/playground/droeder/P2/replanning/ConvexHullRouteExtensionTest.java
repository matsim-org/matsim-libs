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
package playground.droeder.P2.replanning;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.droeder.P2.PScenarioHelper;
import playground.droeder.P2.analysis.Pdata2Shape;

/**
 * @author droeder
 *
 */
public class ConvexHullRouteExtensionTest {

	@Test
    public final void testRun() {
		Cooperative coop = PScenarioHelper.createCoop2111to1314to4443();
		PPlan oldPlan = coop.getBestPlan();
		
		ArrayList<String> parameters = new ArrayList<String>();
		PPlanStrategy strategy = new ConvexHullRouteExtension(parameters);
		coop.init(coop.getRouteProvider(), strategy, 1);
		PPlan newPlan = coop.getBestPlan();
		
		Assert.assertEquals(3, oldPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(oldPlan.getStopsToBeServed().get(0).getId().toString(), "p_2111");
		Assert.assertEquals(oldPlan.getStopsToBeServed().get(1).getId().toString(), "p_1314");
		Assert.assertEquals(oldPlan.getStopsToBeServed().get(2).getId().toString(), "p_4443");
		
		
		Assert.assertEquals(4, newPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(newPlan.getStopsToBeServed().get(0).getId().toString(), "p_2111");
		Assert.assertEquals(newPlan.getStopsToBeServed().get(1).getId().toString(), "p_1314");
		Assert.assertEquals(newPlan.getStopsToBeServed().get(2).getId().toString(), "p_4443");
		Assert.assertEquals(newPlan.getStopsToBeServed().get(3).getId().toString(), "p_1222");
	}

}
