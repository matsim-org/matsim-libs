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

import java.io.File;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.operator.TimeProvider;
import org.matsim.contrib.minibus.replanning.WeightedEndTimeExtension;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;


public class WeightedEndTimeExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Operator coop = PScenarioHelper.createTestCooperative(utils.getOutputDirectory());
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
		
		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "900");
		
		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);
		
		WeightedEndTimeExtension strat = new WeightedEndTimeExtension(new ArrayList<String>());
		strat.setTimeProvider(tP);
		
		PPlan testPlan = null;
		
		coop.getBestPlan().setEndTime(19500.0);

		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// enough vehicles for testing
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 50400.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		// enough vehicles for testing
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 24300.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		// Now same with acts
		Id<Person> agentId = Id.create("id", Person.class);
		Id<Link> linkId = Id.create("id", Link.class);
		Id<ActivityFacility> facilityId = Id.create("id", ActivityFacility.class);

		for (int i = 0; i < 100; i++) {
			tP.handleEvent(new ActivityEndEvent(36600.0, agentId, linkId, facilityId, "type"));
		}
		
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 36000.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		tP.reset(1);
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 47700.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
	}
}