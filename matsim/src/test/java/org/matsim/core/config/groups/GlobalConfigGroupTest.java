/* *********************************************************************** *
 * project: org.matsim.*
 * GlobalConfigGroupTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests for {@link GlobalConfigGroup}.
 */
public class GlobalConfigGroupTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testAllParametersIO() {
		final String file = utils.getOutputDirectory() + "/config.xml";

		// Create config and set all values to non-default
		final Config outConfig = ConfigUtils.createConfig();
		GlobalConfigGroup globalOut = outConfig.global();

		globalOut.setRandomSeed(12345L);
		globalOut.setNumberOfThreads(8);
		globalOut.setCoordinateSystem("EPSG:25832");
		globalOut.setInsistingOnDeprecatedConfigVersion(false);
		globalOut.setDefaultDelimiter(",");
		globalOut.setRelativeToleranceForSampleSizeFactors(0.05);

		// Write config
		new ConfigWriter(outConfig).writeFileV2(file);

		// Read config
		final Config inConfig = ConfigUtils.createConfig();
		new ConfigReader(inConfig).readFile(file);
		GlobalConfigGroup globalIn = inConfig.global();

		// Assert all values are correctly read
		Assertions.assertEquals(12345L, globalIn.getRandomSeed(),
				"randomSeed should be correctly written and read");
		Assertions.assertEquals(8, globalIn.getNumberOfThreads(),
				"numberOfThreads should be correctly written and read");
		Assertions.assertEquals("EPSG:25832", globalIn.getCoordinateSystem(),
				"coordinateSystem should be correctly written and read");
		Assertions.assertFalse(globalIn.isInsistingOnDeprecatedConfigVersion(),
				"insistingOnDeprecatedConfigVersion should be correctly written and read");
		Assertions.assertEquals(",", globalIn.getDefaultDelimiter(),
				"defaultDelimiter should be correctly written and read");
		Assertions.assertEquals(0.05, globalIn.getRelativeToleranceForSampleSizeFactors(), 1e-10,
				"relativeToleranceForSampleSizeFactors should be correctly written and read");
	}

	@Test
	void testDefaultValues() {
		final Config config = ConfigUtils.createConfig();
		GlobalConfigGroup global = config.global();

		Assertions.assertEquals(4711L, global.getRandomSeed(),
				"Default randomSeed should be 4711");
		Assertions.assertEquals(2, global.getNumberOfThreads(),
				"Default numberOfThreads should be 2");
		Assertions.assertEquals("Atlantis", global.getCoordinateSystem(),
				"Default coordinateSystem should be 'Atlantis'");
		Assertions.assertTrue(global.isInsistingOnDeprecatedConfigVersion(),
				"Default insistingOnDeprecatedConfigVersion should be true");
		Assertions.assertEquals(";", global.getDefaultDelimiter(),
				"Default defaultDelimiter should be ';'");
		Assertions.assertEquals(0.0, global.getRelativeToleranceForSampleSizeFactors(), 1e-10,
				"Default relativeToleranceForSampleSizeFactors should be 0.0");
	}
}
