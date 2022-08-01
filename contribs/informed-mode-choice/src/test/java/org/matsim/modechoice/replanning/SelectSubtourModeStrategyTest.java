package org.matsim.modechoice.replanning;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.PrepareForMobsim;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;


public class SelectSubtourModeStrategyTest extends ScenarioTest {

	@Override
	protected String[] getArgs() {
		return new String[]{"--mc"};
	}

	@Test
	public void person() {

		PrepareForMobsim prepare = injector.getInstance(PrepareForMobsim.class);
		prepare.run();


		PlanStrategy strategy = injector.getInstance(Key.get(PlanStrategy.class, Names.named(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)));

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

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