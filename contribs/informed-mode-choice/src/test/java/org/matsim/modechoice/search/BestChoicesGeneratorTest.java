package org.matsim.modechoice.search;

import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class BestChoicesGeneratorTest extends ScenarioTest {

	@Test
	public void choices() {

		BestChoicesGenerator generator = injector.getInstance(BestChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());
		assertThat(candidates)
				.hasSize(1)
				.first()
				.isEqualTo(new PlanCandidate(new String[]{"bike", "walk", "pt", "walk"}, Double.NaN));


		person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));
		candidates = generator.generate(person.getSelectedPlan());
		assertThat(candidates)
				.hasSize(1)
				.first()
				.isEqualTo(new PlanCandidate(new String[]{"car", "car", "car", "car", "car", "car", "car"}, Double.NaN));
	}

}