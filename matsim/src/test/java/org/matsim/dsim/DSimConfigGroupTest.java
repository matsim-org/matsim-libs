package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DSimConfigGroupTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void taskSchedulingDefaultsToBackoff() {
		assertEquals(DSimConfigGroup.TaskScheduling.backoff, new DSimConfigGroup().getTaskScheduling());
	}

	@Test
	void taskSchedulingRoundTripsThroughXml() {
		String filename = utils.getOutputDirectory() + "config.xml";

		Config writeConfig = ConfigUtils.createConfig();
		writeConfig.dsim().setTaskScheduling(DSimConfigGroup.TaskScheduling.eager);
		ConfigUtils.writeConfig(writeConfig, filename);

		Config readConfig = ConfigUtils.loadConfig(filename);
		assertEquals(DSimConfigGroup.TaskScheduling.eager, readConfig.dsim().getTaskScheduling());
	}
}
