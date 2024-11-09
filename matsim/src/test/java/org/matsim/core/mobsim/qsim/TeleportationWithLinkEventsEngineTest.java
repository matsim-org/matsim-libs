package org.matsim.core.mobsim.qsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class TeleportationWithLinkEventsEngineTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testEventsEqualWithRoutedTeleporting() {

		URL kelheim = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = utils.loadConfig(IOUtils.extendUrl(kelheim, "config.xml"));

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Scenario scenario = ScenarioUtils.loadScenario(config);

		new Controler(scenario).run();

	}


	private void prepareScenario(Scenario scenario) {
		// TODO adjust freight network
		// insert activities params
	}

}
