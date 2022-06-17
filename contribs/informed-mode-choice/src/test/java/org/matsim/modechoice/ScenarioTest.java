package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.Before;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class ScenarioTest {

	protected InformedModeChoiceConfigGroup group;
	protected Controler controler;

	protected Injector injector;

	@Before
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig();

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		controler = MATSimApplication.prepare(TestScenario.class, config);
		injector = controler.getInjector();

	}

}
