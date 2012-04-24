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
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;



/**
 * @author droeder
 *
 */
public class RandomRouteEndExtensionTest {
	
	
	@Test
    public final void testRun() {
		MatsimRandom.reset();
		Cooperative coop = PScenarioHelper.createCoop2111to2333();
		PPlan oldPlan = coop.getBestPlan();
		
		// test plan should change
		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("0.7");
		PPlanStrategy strategy = new RandomRouteEndExtension(parameters);
		coop.init(coop.getRouteProvider(), strategy, 1);
		
		PPlan newPlan = coop.getBestPlan();
		
		Assert.assertEquals("p_2111", oldPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("p_2333", oldPlan.getStopsToBeServed().get(1).getId().toString());
		
		
		Assert.assertEquals(3, newPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("p_2111", newPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("p_2333", newPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("p_3444", newPlan.getStopsToBeServed().get(2).getId().toString());
		
		String[] oldPlanLinks = {"2111", "1112", "1222", "2223", "2333", "3332", "3231", "3121", "2111"};
		for(TransitRoute r: oldPlan.getLine().getRoutes().values()){
			for(int i = 0; i < r.getStops().size(); i++){
				Assert.assertEquals( oldPlanLinks[i], r.getStops().get(i).getStopFacility().getLinkId().toString());
			}
		}
		
		String[] newPlanLinks = {"2111", "1112", "1222", "2223", "2333", "3334", "3444", "4434", "3433", "3332", "3222", "2221", "2111"};
		for(TransitRoute r: newPlan.getLine().getRoutes().values()){
			for(int i = 0; i < r.getStops().size(); i++){
				Assert.assertEquals(newPlanLinks[i], r.getStops().get(i).getStopFacility().getLinkId().toString());
			}
		}
		
		//test plan should stay the same
		Cooperative coop2 = PScenarioHelper.createCoop2111to2333();
		oldPlan = coop2.getBestPlan();
		
		parameters = new ArrayList<String>();
		parameters.add("0.5");
		strategy = new RandomRouteEndExtension(parameters);
		coop2.init(coop2.getRouteProvider(), strategy, 1);
		
		newPlan = coop2.getBestPlan();
		
		Assert.assertEquals(2, oldPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(oldPlan.getStopsToBeServed().get(0).getId().toString(), "p_2111");
		Assert.assertEquals(oldPlan.getStopsToBeServed().get(1).getId().toString(), "p_2333");
		
		
		Assert.assertEquals(2, newPlan.getStopsToBeServed().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(newPlan.getStopsToBeServed().get(0).getId().toString(), "p_2111");
		Assert.assertEquals(newPlan.getStopsToBeServed().get(1).getId().toString(), "p_2333");
		
		for(TransitRoute r: oldPlan.getLine().getRoutes().values()){
			for(int i = 0; i < r.getStops().size(); i++){
				Assert.assertEquals(oldPlanLinks[i], r.getStops().get(i).getStopFacility().getLinkId().toString());
			}
		}
		
		String[] newPlanLinks2 = {"2111", "1112", "1222", "2223", "2333", "3332", "3231", "3121", "2111"};
		for(TransitRoute r: newPlan.getLine().getRoutes().values()){
			for(int i = 0; i < r.getStops().size(); i++){
				Assert.assertEquals(newPlanLinks2[i], r.getStops().get(i).getStopFacility().getLinkId().toString());
			}
		}
	}

}
