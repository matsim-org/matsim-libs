package org.matsim.modechoice.pruning;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatePrunerTest {

	private final CandidatePruner pruner = new ExamplePruner(TransportMode.drt);

	@Test
	void example() {

		PlanModel model = Mockito.mock(PlanModel.class);

		List<PlanCandidate> candidates = new ArrayList<>(List.of(
			new PlanCandidate(new String[]{"car", "car", "car", "car"}, Double.NaN),
			new PlanCandidate(new String[]{"car", "bike", "pt", "car"}, Double.NaN),
			new PlanCandidate(new String[]{"car", "drt", "car", "car"}, Double.NaN),
			new PlanCandidate(new String[]{"drt"}, Double.NaN)
		));

		pruner.pruneCandidates(model, candidates, new Random());

		assertThat(candidates)
			.hasSize(2);

	}

}
