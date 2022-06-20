package org.matsim.modechoice.replanning;

import org.junit.Before;
import org.junit.Test;
import org.matsim.modechoice.PlanCandidate;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MultinomialLogitSelectorTest {

	private MultinomialLogitSelector selector;

	@Before
	public void setUp() throws Exception {
		selector = new MultinomialLogitSelector(0.1, new Random(0));
	}

	@Test
	public void selection() {

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -1),
				new PlanCandidate(new String[]{"pt"}, -1.5),
				new PlanCandidate(new String[]{"walk"}, -2)
		);


		double[] sample = sample(1000, candidates);

		assertThat(sample)
				.containsExactly(0.51, 0.306, 0.184);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -50),
				new PlanCandidate(new String[]{"pt"}, -60),
				new PlanCandidate(new String[]{"walk"}, -70)
		);

		sample = sample(1000, candidates);

		assertThat(sample)
				.containsExactly(1, 0, 0);


		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 7),
				new PlanCandidate(new String[]{"pt"}, 6),
				new PlanCandidate(new String[]{"walk"}, 5)
		);

		sample = sample(1000, candidates);

		assertThat(sample)
				.containsExactly(0.651, 0.26, 0.089);

	}


	/**
	 * Sample n times and return relative frequency of how often a candidate was selected.
	 */
	private double[] sample(int n, List<PlanCandidate> candidates) {

		double[] res = new double[candidates.size()];

		for (int i = 0; i < n; i++) {
			res[candidates.indexOf(selector.select(candidates))]++;
		}

		for (int i = 0; i < res.length; i++) {
			res[i] /= n;
		}

		return res;
	}

}