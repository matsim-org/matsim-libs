package org.matsim.modechoice.search;

import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;


public class SingleTripChoicesGeneratorTest extends ScenarioTest {

	@Test
	public void choices() {

		SingleTripChoicesGenerator generator = injector.getInstance(SingleTripChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan(), new boolean[]{true, false, false, false});

		assertThat(candidates)
				.first().matches(c -> c.getMode(0).equals(TransportMode.car));

		assertThat(candidates)
				.last().matches(c -> c.getMode(0).equals(TransportMode.ride));

	}


	@Test
	public void unavailable() {

		SingleTripChoicesGenerator generator = injector.getInstance(SingleTripChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan(), new boolean[]{false, false, true, false, false, false, false});


		assertThat(candidates)
				.noneMatch(c -> c.getMode(2).equals(TransportMode.pt));

	}
}