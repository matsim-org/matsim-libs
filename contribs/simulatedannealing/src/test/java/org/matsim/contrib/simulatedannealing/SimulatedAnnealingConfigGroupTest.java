/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.simulatedannealing.temperature.TemperatureFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class SimulatedAnnealingConfigGroupTest {

	@TempDir
	public File tempFolder;

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig();

		SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();
		simAnCfg.initialTemperature = 42;
		simAnCfg.alpha = 42;
		simAnCfg.coolingSchedule = TemperatureFunction.DefaultFunctions.exponentialAdditive;

		config.addModule(simAnCfg);
		return config;
	}


	private static Path writeConfig(final File tempFolder) throws IOException {
		Config config = createConfig();
		Path configFile = new File(tempFolder,"config.xml").toPath();
		ConfigUtils.writeConfig(config, configFile.toString());
		return configFile;
	}

	@Test
	void loadConfigGroupTest() throws IOException {

		/* Test that exported values are correct imported again */
		Path configFile = writeConfig(tempFolder);
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFile.toString());
		SimulatedAnnealingConfigGroup loadedCfg = ConfigUtils.addOrGetModule(config, SimulatedAnnealingConfigGroup.class);
		Assertions.assertEquals(42., loadedCfg.alpha, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(42., loadedCfg.initialTemperature, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(TemperatureFunction.DefaultFunctions.exponentialAdditive, loadedCfg.coolingSchedule);
	}


	@Test
	void perturbationParamsTest() {
		Config config = createConfig();
		SimulatedAnnealingConfigGroup saConfig = ConfigUtils.addOrGetModule(config, SimulatedAnnealingConfigGroup.class);

		Assertions.assertTrue(saConfig.getPerturbationParams().isEmpty());

		saConfig.addPerturbationParams(new SimulatedAnnealingConfigGroup.PerturbationParams("perturb", 1.) {
			@Override
			public Map<String, String> getComments() {
				return super.getComments();
			}
		});

		Assertions.assertFalse(saConfig.getPerturbationParams().isEmpty());
		Assertions.assertTrue(saConfig.getPerturbationParamsPerType().containsKey("perturb"));
		Assertions.assertEquals(1., saConfig.getPerturbationParamsPerType().get("perturb").weight, MatsimTestUtils.EPSILON);

	}
}
