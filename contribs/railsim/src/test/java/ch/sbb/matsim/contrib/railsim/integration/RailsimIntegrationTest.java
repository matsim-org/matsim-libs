package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

public class RailsimIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void scenario0() {

		File dir = new File(utils.getPackageInputDirectory(), "test0");

		Config config = ConfigUtils.loadConfig(new File(dir, "config.xml").toString());

		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RailsimModule());

		/*
		controler.configureQSimComponents(components -> {
			new RailsimQSimModule().configure(components);
		});
		 */

		controler.run();

	}
}
