package org.matsim.modechoice.search;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.PrepareForMobsim;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TopKSubtourGeneratorTest extends ScenarioTest {

	@Test
	void subtours() {

		// Subtours need mapped locations
		PrepareForMobsim prepare = injector.getInstance(PrepareForMobsim.class);
		prepare.run();

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

		PlanModel planModel = PlanModel.newInstance(person.getSelectedPlan());


		Collection<PlanCandidate> candidates = generator.generate(planModel, null, new boolean[]{true, true, false, false, false, false, false});

		PlanCandidate first = candidates.iterator().next();

		assertThat(candidates)
				.first().matches(f -> f.getMode(0).equals(TransportMode.car) && f.getMode(1).equals(TransportMode.car));

		List<String[]> modes = new ArrayList<>();
		modes.add(first.getModes());

		List<PlanCandidate> result = generator.generatePredefined(planModel, modes);

		assertThat(first).isEqualTo(result.get(0));
		assertThat(first.getUtility()).isEqualTo(result.get(0).getUtility());

	}
}