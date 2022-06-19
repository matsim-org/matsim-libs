package org.matsim.modechoice.search;

import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class TopKChoicesGeneratorTest extends ScenarioTest {


	@Test
	public void choices() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());

		assertThat(candidates)
				.hasSize(10)
				.first()
				.isEqualTo(new PlanCandidate(new String[]{"car", "car", "car", "car", "car", "car", null}, Double.NaN));


	}
}