package org.matsim.modechoice.replanning;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.matsim.modechoice.PlanCandidate;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MultinomialLogitSelectorTest {

	private MultinomialLogitSelector selector;

	private static final int N = 100_000;

	@Before
	public void setUp() throws Exception {
		selector = new MultinomialLogitSelector(1, new Random(0));
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
				.containsExactly(0.491, 0.323, 0.186);


		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 7),
				new PlanCandidate(new String[]{"pt"}, 7),
				new PlanCandidate(new String[]{"walk"}, 5)
		);

		sample = sample(N, candidates);

		assertThat(sample)
				.containsExactly(0.42304, 0.42012, 0.15684);

	}

	@Test
	public void invariance() {

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -1),
				new PlanCandidate(new String[]{"pt"}, -2),
				new PlanCandidate(new String[]{"walk"}, -3)
		);


		double[] sample1 = sample(N, candidates);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -10),
				new PlanCandidate(new String[]{"pt"}, -20),
				new PlanCandidate(new String[]{"walk"}, -30)
		);

		double[] sample2 = sample(N, candidates);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 10),
				new PlanCandidate(new String[]{"pt"}, 0),
				new PlanCandidate(new String[]{"walk"}, -10)
		);

		double[] sample3 = sample(N, candidates);

		assertThat(sample1)
				.containsExactly(0.50819, 0.30373, 0.18808);
		assertThat(sample2)
				.containsExactly(0.50656, 0.30717, 0.18627);
		assertThat(sample3)
				.containsExactly(0.50239, 0.30943, 0.18818);

	}

	@Test
	public void best() {

		selector = new MultinomialLogitSelector(0, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 1.04),
				new PlanCandidate(new String[]{"pt"}, 1.06),
				new PlanCandidate(new String[]{"walk"}, 1.05)
		);

		double[] sample = sample(N, candidates);

		assertThat(sample).containsExactly(0, 1, 0);

	}

	@Test
	public void sameScore() {

		selector = new MultinomialLogitSelector(0.001, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 1.),
				new PlanCandidate(new String[]{"walk"}, 1.)
		);

		double[] sample = sample(N, candidates);

		assertThat(sample[0]).isCloseTo(0.5, Offset.offset(0.01));
		assertThat(sample[1]).isCloseTo(0.5, Offset.offset(0.01));
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