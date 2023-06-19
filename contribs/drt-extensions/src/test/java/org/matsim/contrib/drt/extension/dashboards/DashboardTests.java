package org.matsim.contrib.drt.extension.dashboards;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.extension.DrtTestScenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

public class DashboardTests {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private void run() {

		Config config = DrtTestScenario.loadConfig(utils);
		config.controler().setLastIteration(2);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.defaultParams().sampleSize = "0.001";

		Controler controler = MATSimApplication.prepare(new DrtTestScenario(config), config);
		controler.run();
	}

	@Test
	public void drtDefaults() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "drt");

		run();

		// TODO: add assertions
	}
}
