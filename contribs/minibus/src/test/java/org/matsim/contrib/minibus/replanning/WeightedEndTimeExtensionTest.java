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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.hook.Operator;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.hook.TimeProvider;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;


public class WeightedEndTimeExtensionTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testRun() {

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

		Assertions.assertEquals(1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals(19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
		Assertions.assertNull(testPlan, "Test plan should be null");

		coop.getBestPlan().setNVehicles(2);

		// enough vehicles for testing
		testPlan = strat.run(coop);

		Assertions.assertEquals(2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals(19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
		Assertions.assertNotNull(testPlan, "Test plan should be not null");
		Assertions.assertEquals(1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON, "There should be one vehicle bought");
		Assertions.assertEquals(50400.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");

		// enough vehicles for testing
		testPlan = strat.run(coop);

		Assertions.assertEquals(2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals(19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
		Assertions.assertNotNull(testPlan, "Test plan should be not null");
		Assertions.assertEquals(1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON, "There should be one vehicle bought");
		Assertions.assertEquals(24300.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");

		// Now same with acts
		Id<Person> agentId = Id.create("id", Person.class);
		Id<Link> linkId = Id.create("id", Link.class);
		Id<ActivityFacility> facilityId = Id.create("id", ActivityFacility.class);

		for (int i = 0; i < 100; i++) {
			tP.handleEvent(new ActivityEndEvent(36600.0, agentId, linkId, facilityId, "type"));
		}

		testPlan = strat.run(coop);

		Assertions.assertEquals(2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals(19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
		Assertions.assertNotNull(testPlan, "Test plan should be not null");
		Assertions.assertEquals(1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON, "There should be one vehicle bought");
		Assertions.assertEquals(36000.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");

		tP.reset(1);
		testPlan = strat.run(coop);

		Assertions.assertEquals(2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals(19500.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
		Assertions.assertNotNull(testPlan, "Test plan should be not null");
		Assertions.assertEquals(1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON, "There should be one vehicle bought");
		Assertions.assertEquals(47700.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON, "Compare start time");
	}
}
