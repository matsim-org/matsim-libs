/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesParserWriterTest.java
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

package org.matsim.facilities;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.testcases.MatsimTestUtils;

public class FacilitiesParserWriterTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testParserWriter1() {
		Config config = ConfigUtils.createConfig();
		TriangleScenario.setUpScenarioConfig(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());

		String outputFilename = this.utils.getOutputDirectory() + "output_facilities.xml";
		TriangleScenario.writeFacilities(facilities, outputFilename);

		long checksum_ref = CRCChecksum.getCRCFromFile(config.facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(outputFilename);
		Assert.assertEquals(checksum_ref, checksum_run);
	}

}
