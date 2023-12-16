package org.matsim.modechoice.search;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class SingleTripChoicesGeneratorTest extends ScenarioTest {

	@Test
	void choices() {

		SingleTripChoicesGenerator generator = injector.getInstance(SingleTripChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		Collection<PlanCandidate> candidates = generator.generate(model, null, new boolean[]{true, false, false, false});

		assertThat(candidates)
				.first().matches(c -> c.getMode(0).equals(TransportMode.car));

		assertThat(candidates)
				.last().matches(c -> c.getMode(0).equals(TransportMode.ride));

	}


	@Test
	void unavailable() {

		SingleTripChoicesGenerator generator = injector.getInstance(SingleTripChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		Collection<PlanCandidate> candidates = generator.generate(model, null, new boolean[]{false, false, true, false, false, false, false});

		System.out.println(candidates);

		assertThat(candidates)
				.noneMatch(c -> c.getMode(2).equals(TransportMode.pt));

	}

	@Test
	void subset() {

		SingleTripChoicesGenerator generator = injector.getInstance(SingleTripChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		Collection<PlanCandidate> candidates = generator.generate(model, Set.of(TransportMode.car, TransportMode.walk), new boolean[]{true, false, false, false});

		assertThat(candidates)
				.hasSize(2)
				.first().matches(c -> c.getMode(0).equals(TransportMode.car));

	}
}