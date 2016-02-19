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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;

public class ConfigParameterTest extends MatsimTestCase {

	public void testParametersSetCorrectly() {
		Config config = super.loadConfig(this.getPackageInputDirectory() + "config.xml");
		JDEQSimConfigGroup jdeqSimConfigGroup = ConfigUtils.addOrGetModule(config, JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class);
		assertEquals(360.0, jdeqSimConfigGroup.getSimulationEndTime(), EPSILON);
		assertEquals(2.0, jdeqSimConfigGroup.getFlowCapacityFactor(), EPSILON);
		assertEquals(3.0, jdeqSimConfigGroup.getStorageCapacityFactor(), EPSILON);
		assertEquals(3600.0, jdeqSimConfigGroup.getMinimumInFlowCapacity(), EPSILON);
		assertEquals(10.0, jdeqSimConfigGroup.getCarSize(), EPSILON);
		assertEquals(20.0, jdeqSimConfigGroup.getGapTravelSpeed(), EPSILON);
		assertEquals(9000.0, jdeqSimConfigGroup.getSqueezeTime(), EPSILON);
	}
}
