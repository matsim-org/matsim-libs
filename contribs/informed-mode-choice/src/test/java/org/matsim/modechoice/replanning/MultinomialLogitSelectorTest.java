package org.matsim.modechoice.replanning;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.modechoice.PlanCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MultinomialLogitSelectorTest {

	private static final int N = 500_000;
	private final Offset<Double> offset = Offset.offset(0.01);
	private MultinomialLogitSelector selector;

	@BeforeEach
	public void setUp() throws Exception {
		selector = new MultinomialLogitSelector(1, new Random(0));
	}

	private double[] sample(double... utils) {
		List<PlanCandidate> candidates = new ArrayList<>();

		for (int i = 0; i < utils.length; i++) {
			candidates.add(new PlanCandidate(new String[]{"" + i}, utils[i]));
		}

		return selector.sample(N, candidates);
	}

	@Test
	void probabilities() {

		// checked with https://docs.scipy.org/doc/scipy/reference/generated/scipy.special.softmax.html

		assertThat(sample(-1, -2, -3))
			.containsExactly(new double[]{0.66524096, 0.24472847, 0.09003057}, offset);

		assertThat(sample(-3, -1, -2))
			.containsExactly(new double[]{0.09003057, 0.66524096, 0.24472847}, offset);

		assertThat(sample(-2, -4, -6))
			.containsExactly(new double[]{0.86681333, 0.11731043, 0.01587624}, offset);

		assertThat(sample(-0.5, -1, -1.5))
			.containsExactly(new double[]{0.50648039, 0.30719589, 0.18632372}, offset);

		selector = new MultinomialLogitSelector(2, new Random(0));

		assertThat(sample(-1, -2, -3))
			.containsExactly(new double[]{0.50648039, 0.30719589, 0.18632372}, offset);

		selector = new MultinomialLogitSelector(0.5, new Random(0));

		assertThat(sample(-1, -2, -3))
			.containsExactly(new double[]{0.86681333, 0.11731043, 0.01587624}, offset);

		selector = new MultinomialLogitSelector(10, new Random(0));

		assertThat(sample(-1, -2, -3))
			.containsExactly(new double[]{0.3671654 , 0.33222499, 0.30060961}, offset);

	}

	@Test
	void best() {

		selector = new MultinomialLogitSelector(0, new Random(0));

		assertThat(sample(1.04, 1.06, 1.05))
			.containsExactly(0, 1, 0);

	}

	@Test
	void random() {

		selector = new MultinomialLogitSelector(Double.POSITIVE_INFINITY, new Random(0));
		assertThat(sample(100, 3, 1))
			.containsExactly(new double[]{0.333, 0.333, 0.333}, offset);

	}

	@Test
	void invariance() {

		// Selector is invariant to shifting, but not to scale of utilities
		assertThat(sample(-1, -2, -3))
			.containsExactly(new double[]{0.664678, 0.244972, 0.09035}, offset);

		assertThat(sample(1, 0, -1))
			.containsExactly(new double[]{0.665126, 0.244834, 0.09004}, offset);

	}

	@Test
	void smallScale() {

		selector = new MultinomialLogitSelector(1 / 10000d, new Random(0));

		assertThat(sample(-1e-4, -1e-4, -2e-4))
			.containsExactly(new double[]{0.4223188, 0.4223188, 0.1553624}, offset);

		assertThat(sample(-1, -1, -2))
			.containsExactly(new double[]{1/2d, 1/2d, 0}, offset);

		selector = new MultinomialLogitSelector(2e-8, new Random(0));

		assertThat(sample(-1, -1.0001, -2))
			.containsExactly(new double[]{1, 0, 0}, offset);

	}
}
