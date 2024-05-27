package org.matsim.modechoice.replanning;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;


public class SelectSingleTripModeStrategyTest extends ScenarioTest {

	@Test
	void selectSingleTrip() {

		Config config = TestScenario.loadConfig(utils);

		config.controller().setLastIteration(10);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(InformedModeChoiceModule.SELECT_SINGLE_TRIP_MODE_STRATEGY)
				.setSubpopulation("person")
				.setWeight(0.5)
		);

		ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class)
				.setAnneal(InformedModeChoiceConfigGroup.Schedule.linear);

		Controler controler = MATSimApplication.prepare(TestScenario.class, config);

		controler.run();

	}

	@Test
	void person() {

		PlanStrategy strategy = injector.getInstance(Key.get(PlanStrategy.class, Names.named(InformedModeChoiceModule.SELECT_SINGLE_TRIP_MODE_STRATEGY)));

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		person.getPlans().removeIf(p -> person.getSelectedPlan() != p);

		run(strategy, person);
		run(strategy, person);
		run(strategy, person);
		run(strategy, person);


	}

	private void run(PlanStrategy strategy, Person person) {
		strategy.init(() -> 1);
		strategy.run(person);
		strategy.finish();
	}

}
