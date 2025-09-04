package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.testcases.MatsimTestUtils;

public class ScenarioTest {

	protected InformedModeChoiceConfigGroup group;
	protected Controler controler;
	protected Injector injector;

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@BeforeEach
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig(utils);

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		prepareConfig(config);

		controler = MATSimApplication.prepare(TestScenario.class, config, getArgs());
		injector = controler.getInjector();
		injector.getInstance( PrepareForSim.class ).run();
	}

	protected void prepareConfig(Config config) {
	}

	protected String[] getArgs() {
		return new String[0];
	}

}
