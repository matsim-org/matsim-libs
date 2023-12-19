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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.hook.TimeProvider;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;


public class TimeProviderTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testGetRandomTimeInIntervalOneTimeSlot() {

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "99999999");

		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);

		double startTime = 3600.0;
		double endTime = startTime;

		Assertions.assertEquals(0.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "New time (There is only one slot, thus time is zero)");
	}

	@Test
	final void testGetRandomTimeInIntervalOneSameStartEndTime() {

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "900");

		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);

		double startTime = 3600.0;
		double endTime = startTime;

		Assertions.assertEquals(3600.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Same start and end time. Should return start time");
	}

	@Test
	final void testGetRandomTimeInIntervalDifferentStartEndTime() {

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "900");

		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);

		double startTime = 7500.0;
		double endTime = 19400.0;

		Assertions.assertEquals(7200.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");
		Assertions.assertEquals(11700.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");

		Id<Person> agentId = Id.create("id", Person.class);
		Id<Link> linkId = Id.create("id", Link.class);
		Id<ActivityFacility> facilityId = Id.create("id", ActivityFacility.class);
		for (int i = 0; i < 100; i++) {
			tP.handleEvent(new ActivityEndEvent(500.0 * i, agentId, linkId, facilityId, "type"));
		}

		Assertions.assertEquals(9000.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");
		Assertions.assertEquals(10800.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");

		tP.reset(1);
		Assertions.assertEquals(11700.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");
		Assertions.assertEquals(9900.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON, "Check time");
	}
}
