package org.matsim.modechoice.search;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class DifferentModesTest extends ScenarioTest {


	@Test
	void topK() {

		group.setTopK(1024);

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		group.setRequireDifferentModes(false);

		Collection<PlanCandidate> candidates = generator.generate(model);

		assertThat(candidates).
				extracting(PlanCandidate::getModes).contains(model.getCurrentModes());

	}
}
