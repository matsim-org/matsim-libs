package org.matsim.simwrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class SimWrapperModuleTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runScenario() {

		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(5);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();

	}
}
