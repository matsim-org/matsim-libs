package org.matsim.modechoice.replanning;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.PrepareForMobsim;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.modechoice.*;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class SelectSubtourModeStrategyTest extends ScenarioTest {

	@Override
	protected String[] getArgs() {
		return new String[]{"--mc"};
	}

	@Test
	void person() {

		PrepareForMobsim prepare = injector.getInstance(PrepareForMobsim.class);
		prepare.run();


		PlanStrategy strategy = injector.getInstance(Key.get(PlanStrategy.class, Names.named(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)));

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(5));

		person.getPlans().removeIf(p -> person.getSelectedPlan() != p);

		for (int i = 0; i < 20; i++) {
			run(strategy, person);
		}

	}

	@Test
	void constraint() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		List<String[]> modes = new ArrayList<>();

		modes.add(new String[]{TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car});
		modes.add(new String[]{TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.walk, TransportMode.car, TransportMode.car, TransportMode.car});

		List<PlanCandidate> result = generator.generatePredefined(model, modes);

		assertThat(result.get(0).getUtility())
				.isEqualTo(-7.8, Offset.offset(0.1));

		assertThat(result.get(1).getUtility())
				.isEqualTo(Double.NEGATIVE_INFINITY);

	}

	@Test
	void allowedModes() {


		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		// This agent is not allowed to use car
		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(6));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		System.out.println(model);

		List<String[]> modes = new ArrayList<>();

		modes.add(new String[]{TransportMode.walk, TransportMode.walk});
		modes.add(new String[]{TransportMode.car, TransportMode.car});

		List<PlanCandidate> result = generator.generatePredefined(model, modes);

		assertThat(result.get(0).getUtility())
				.isEqualTo(-27.8, Offset.offset(0.1));

		assertThat(result.get(1).getUtility())
				.isEqualTo(Double.NEGATIVE_INFINITY);

	}

	private void run(PlanStrategy strategy, Person person) {
		strategy.init(() -> 1);
		strategy.run(person);
		strategy.finish();
	}

}