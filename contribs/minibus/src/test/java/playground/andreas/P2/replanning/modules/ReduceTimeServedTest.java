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
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.deprecated.ReduceTimeServed;


public class ReduceTimeServedTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		PConfigGroup pC = new PConfigGroup();
		
		Cooperative coop = PScenarioHelper.createCoop2111to1314to4443();
		ArrayList<String> param = new ArrayList<String>();
		param.add("1.0");
		param.add("700");
		ReduceTimeServed strat = new ReduceTimeServed(param);
		strat.setPIdentifier(pC.getPIdentifier());
		
		double startTime = 8 * 3600.0;
		double endTime = 16 * 3600.0;
		
		Assert.assertEquals("Start time", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		
		Id veh1 = new IdImpl(pC.getPIdentifier() + 1 + "-" + 1);
		Id lineId = coop.getId();
		
		strat.handleEvent(new TransitDriverStartsEvent(18000.0, new IdImpl("d1"), veh1, lineId, new IdImpl("route1"), new IdImpl("dep1")));

		PPlan testPlan = null;
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		// too few vehicles for testing - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// enough vehicles for testing but still no demand - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);		
		
		
		// generate 100 trips from 12:00 to 15:00
		Set<Id> personIds = new TreeSet<Id>();		
		
		for (int i = 0; i < 100; i++) {
			Id personId = new IdImpl(i);
			personIds.add(personId);
			strat.handleEvent(new PersonEntersVehicleEvent(12 * 3600.0, personId, veh1));
		}
		
		Assert.assertEquals("Number of test persons", 100, personIds.size(), MatsimTestUtils.EPSILON);
		
		for (Id personId : personIds) {
			strat.handleEvent(new PersonLeavesVehicleEvent(15 * 3600.0, personId, veh1));
		}
				
		
		// should be able to replan
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time best plan", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time best plan", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time test plan", 42700.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time test plan", 54600.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		
		// add a few other plans
		for (int i = 0; i < 10; i++) {
			strat.handleEvent(new PersonEntersVehicleEvent(10 * 3600.0, new IdImpl("11" + i), veh1));
			strat.handleEvent(new PersonLeavesVehicleEvent(18 * 3600.0, new IdImpl("11" + i), veh1));			
		}
		
		// should be able to replan, but with same result
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time best plan", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time best plan", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time test plan", 42700.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time test plan", 54600.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		// add a lot of new plans
		for (int i = 0; i < 100; i++) {
			strat.handleEvent(new PersonEntersVehicleEvent(9 * 3600.0, new IdImpl("12" + i), veh1));
			strat.handleEvent(new PersonLeavesVehicleEvent(13 * 3600.0, new IdImpl("12" + i), veh1));			
		}
		
		// should be able to replan, but with an earlier start time
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time best plan", startTime, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time best plan", endTime, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start time test plan", 32200.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("End time test plan", 54600.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
	}
}