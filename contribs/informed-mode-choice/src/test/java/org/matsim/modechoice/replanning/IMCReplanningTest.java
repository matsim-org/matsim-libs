package org.matsim.modechoice.replanning;

import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.TestScenario;

public class IMCReplanningTest {

	@Test
	public void replanning() {

		Config config = TestScenario.loadConfig();

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