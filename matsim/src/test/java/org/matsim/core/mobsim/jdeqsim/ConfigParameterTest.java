/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ConfigParameterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testParametersSetCorrectly() {
		Config config = utils.loadConfig(utils.getPackageInputDirectory() + "config.xml");
		JDEQSimConfigGroup jdeqSimConfigGroup = ConfigUtils.addOrGetModule(config, JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class);
		assertEquals(360.0, jdeqSimConfigGroup.getSimulationEndTime().seconds(), MatsimTestUtils.EPSILON);
		assertEquals(2.0, jdeqSimConfigGroup.getFlowCapacityFactor(), MatsimTestUtils.EPSILON);
		assertEquals(3.0, jdeqSimConfigGroup.getStorageCapacityFactor(), MatsimTestUtils.EPSILON);
		assertEquals(3600.0, jdeqSimConfigGroup.getMinimumInFlowCapacity(), MatsimTestUtils.EPSILON);
		assertEquals(10.0, jdeqSimConfigGroup.getCarSize(), MatsimTestUtils.EPSILON);
		assertEquals(20.0, jdeqSimConfigGroup.getGapTravelSpeed(), MatsimTestUtils.EPSILON);
		assertEquals(9000.0, jdeqSimConfigGroup.getSqueezeTime(), MatsimTestUtils.EPSILON);
	}
}
