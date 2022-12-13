package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Rule;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class ScenarioTest {

	protected InformedModeChoiceConfigGroup group;
	protected Controler controler;
	protected Injector injector;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig(utils);

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		prepareConfig(config);

		controler = MATSimApplication.prepare(TestScenario.class, config, getArgs());
		injector = controler.getInjector();

	}

	protected void prepareConfig(Config config) {
	}

	protected String[] getArgs() {
		return new String[0];
	}

}
