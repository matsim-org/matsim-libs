package org.matsim.modechoice.replanning.scheduled;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.modechoice.*;

import static org.junit.jupiter.api.Assertions.*;

class AllBestPlansStrategyTest extends ScenarioTest {

	@Override
	protected void prepareConfig(Config config) {

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(20);

		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings()
			.setStrategyName(ScheduledModeChoiceModule.ALL_BEST_K_PLAN_MODES_STRATEGY)
			.setSubpopulation("person")
			.setWeight(0.5)
		);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.9);

		ScheduledModeChoiceConfigGroup smc = ConfigUtils.addOrGetModule(config, ScheduledModeChoiceConfigGroup.class);
		smc.setAdjustTargetIterations(true);
		smc.setScheduleIterations(15);
		smc.setSubpopulations("person");

	}

	@Override
	protected void prepareController(Controler controler) {

		controler.addOverridingModule(
			ScheduledModeChoiceModule.newBuilder()
			.build()
		);

	}

	@Test
	@Disabled("This test runs a bit longer and is for manual testing.")
	void run() {

		controler.run();

	}

}
