/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class PlansCalcRouteConfigGroupTest {

	private final static Logger log = Logger.getLogger(PlansCalcRouteConfigGroupTest.class);

	@Test
	public void testBackwardsCompatibility() {
		PlansCalcRouteConfigGroup group = new PlansCalcRouteConfigGroup();

		// test default
		Assert.assertEquals("different default than expected.", 3.0 / 3.6, group.getWalkSpeed(), MatsimTestUtils.EPSILON);
		try {
			group.addParam("walkSpeedFactor", "1.5");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
			Assert.assertFalse("Exception-Message should not be empty.", e.getMessage().isEmpty());
		}
		Assert.assertEquals("value should not have changed.", 3.0 / 3.6, group.getWalkSpeed(), MatsimTestUtils.EPSILON);
		group.addParam("walkSpeed", "1.5");
		Assert.assertEquals("value should have changed.", 1.5, group.getWalkSpeed(), MatsimTestUtils.EPSILON);
	}
}
