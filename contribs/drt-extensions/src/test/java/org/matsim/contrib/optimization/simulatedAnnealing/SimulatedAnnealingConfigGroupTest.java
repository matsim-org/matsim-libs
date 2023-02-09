package org.matsim.contrib.optimization.simulatedAnnealing;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.contrib.optimization.simulatedAnnealing.temperature.TemperatureFunction;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Path;

public class SimulatedAnnealingConfigGroupTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig();

		SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();
		simAnCfg.initialTemperature = 42;
		simAnCfg.alpha = 42;
		simAnCfg.coolingSchedule = TemperatureFunction.DefaultFunctions.exponentialAdditive;

		config.addModule(simAnCfg);
		return config;
	}


	private static Path writeConfig(final TemporaryFolder tempFolder) throws IOException {
		Config config = createConfig();
		Path configFile = tempFolder.newFile("config.xml").toPath();
		ConfigUtils.writeConfig(config, configFile.toString());
		return configFile;
	}

	@Test
	public void loadConfigGroupTest() throws IOException {

		/* Test that exported values are correct imported again */
		Path configFile = writeConfig(tempFolder);
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFile.toString());
		SimulatedAnnealingConfigGroup loadedCfg = ConfigUtils.addOrGetModule(config, SimulatedAnnealingConfigGroup.class);
		Assert.assertEquals(42., loadedCfg.alpha, MatsimTestUtils.EPSILON);
		Assert.assertEquals(42., loadedCfg.initialTemperature, MatsimTestUtils.EPSILON);
		Assert.assertEquals(TemperatureFunction.DefaultFunctions.exponentialAdditive, loadedCfg.coolingSchedule);
	}

}
