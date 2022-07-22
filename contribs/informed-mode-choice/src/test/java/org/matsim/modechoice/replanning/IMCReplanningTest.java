package org.matsim.modechoice.replanning;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

public class IMCReplanningTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void informedModeChoice() {

		Config config = TestScenario.loadConfig(utils);

		config.controler().setLastIteration(10);
		config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
				.setStrategyName(InformedModeChoiceModule.INFORMED_MODE_CHOICE)
				.setSubpopulation("person")
				.setWeight(0.5)
		);

		Controler controler = MATSimApplication.prepare(TestScenario.class, config);

		controler.run();

	}
}