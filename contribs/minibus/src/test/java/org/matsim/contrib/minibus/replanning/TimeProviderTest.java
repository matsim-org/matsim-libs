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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.TimeProvider;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;


public class TimeProviderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testGetRandomTimeInIntervalOneTimeSlot() {
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
	
		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "99999999");
		
		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);
		
		double startTime = 3600.0;
		double endTime = startTime;
		
		Assert.assertEquals("New time (There is only one slot, thus time is zero)", 0.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
	}
	
	@Test
    public final void testGetRandomTimeInIntervalOneSameStartEndTime() {
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
	
		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "900");
		
		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);
		
		double startTime = 3600.0;
		double endTime = startTime;
		
		Assert.assertEquals("Same start and end time. Should return start time", 3600.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
	}
	
	@Test
    public final void testGetRandomTimeInIntervalDifferentStartEndTime() {
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
	
		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("timeSlotSize", "900");
		
		TimeProvider tP = new TimeProvider(pConfig, utils.getOutputDirectory());
		tP.reset(0);
		
		double startTime = 7500.0;
		double endTime = 19400.0;
		
		Assert.assertEquals("Check time", 7200.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Check time", 11700.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
		
		Id<Person> agentId = Id.create("id", Person.class);
		Id<Link> linkId = Id.create("id", Link.class);
		Id<ActivityFacility> facilityId = Id.create("id", ActivityFacility.class);
		for (int i = 0; i < 100; i++) {
			tP.handleEvent(new ActivityEndEvent(500.0 * i, agentId, linkId, facilityId, "type"));
		}
		
		Assert.assertEquals("Check time", 9000.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Check time", 10800.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);

		tP.reset(1);
		Assert.assertEquals("Check time", 11700.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Check time", 9900.0, tP.getRandomTimeInInterval(startTime, endTime), MatsimTestUtils.EPSILON);
	}
}