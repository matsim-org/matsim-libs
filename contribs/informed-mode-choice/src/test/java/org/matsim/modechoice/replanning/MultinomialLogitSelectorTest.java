package org.matsim.modechoice.replanning;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.modechoice.PlanCandidate;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MultinomialLogitSelectorTest {

	private MultinomialLogitSelector selector;

	private static final int N = 500_000;

	@BeforeEach
	public void setUp() throws Exception {
		selector = new MultinomialLogitSelector(1, new Random(0));
	}

	@Test
	void selection() {

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -1),
				new PlanCandidate(new String[]{"pt"}, -1.5),
				new PlanCandidate(new String[]{"walk"}, -2)
		);


		double[] sample = selector.sample(1000, candidates);

		assertThat(sample)
				.containsExactly(0.51, 0.306, 0.184);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -50),
				new PlanCandidate(new String[]{"pt"}, -60),
				new PlanCandidate(new String[]{"walk"}, -70)
		);

		sample = selector.sample(1000, candidates);

		assertThat(sample)
				.containsExactly(0.491, 0.323, 0.186);


		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 7),
				new PlanCandidate(new String[]{"pt"}, 7),
				new PlanCandidate(new String[]{"walk"}, 5)
		);

		sample = selector.sample(N, candidates);

		assertThat(sample)
				.containsExactly(0.42231, 0.42174, 0.15595);

	}

	@Test
	void invariance() {

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -1),
				new PlanCandidate(new String[]{"pt"}, -2),
				new PlanCandidate(new String[]{"walk"}, -3)
		);


		double[] sample1 = selector.sample(N, candidates);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, -10),
				new PlanCandidate(new String[]{"pt"}, -20),
				new PlanCandidate(new String[]{"walk"}, -30)
		);

		double[] sample2 = selector.sample(N, candidates);

		candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 10),
				new PlanCandidate(new String[]{"pt"}, 0),
				new PlanCandidate(new String[]{"walk"}, -10)
		);

		double[] sample3 = selector.sample(N, candidates);

		assertThat(sample1)
				.containsExactly(0.50625, 0.306986, 0.186764);
		assertThat(sample2)
				.containsExactly(0.506194, 0.307304, 0.186502);
		assertThat(sample3)
				.containsExactly(0.506376, 0.30703, 0.186594);

	}

	@Test
	void best() {

		selector = new MultinomialLogitSelector(0, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 1.04),
				new PlanCandidate(new String[]{"pt"}, 1.06),
				new PlanCandidate(new String[]{"walk"}, 1.05)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample).containsExactly(0, 1, 0);

	}

	@Test
	void sameScore() {

		selector = new MultinomialLogitSelector(0.01, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 1.),
				new PlanCandidate(new String[]{"walk"}, 1.)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample[0]).isCloseTo(0.5, Offset.offset(0.01));
		assertThat(sample[1]).isCloseTo(0.5, Offset.offset(0.01));
	}

	@Test
	void single() {


		selector = new MultinomialLogitSelector(1, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car"}, 1.)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample[0]).isEqualTo(1);

	}

	@Test
	void precision() {

		selector = new MultinomialLogitSelector(0.01, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car1"}, 2),
				new PlanCandidate(new String[]{"car2"}, 10000),
				new PlanCandidate(new String[]{"car3"}, 1)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample[1]).isEqualTo(1);

	}

	@Test
	void inf() {

		selector = new MultinomialLogitSelector(10000, new Random(0));

		List<PlanCandidate> candidates = List.of(
				new PlanCandidate(new String[]{"car1"}, 2),
				new PlanCandidate(new String[]{"car2"}, 10000),
				new PlanCandidate(new String[]{"car3"}, 1)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample[0]).isEqualTo(0.333, Offset.offset(0.001));
		assertThat(sample[1]).isEqualTo(0.333, Offset.offset(0.001));
		assertThat(sample[2]).isEqualTo(0.333, Offset.offset(0.001));

	}

	@Test
	void random() {

		selector = new MultinomialLogitSelector(Double.POSITIVE_INFINITY, new Random(0));

		List<PlanCandidate> candidates = List.of(
			new PlanCandidate(new String[]{"car1"}, 100),
			new PlanCandidate(new String[]{"car2"}, 3),
			new PlanCandidate(new String[]{"car3"}, 1)
		);

		double[] sample = selector.sample(N, candidates);

		assertThat(sample[0]).isEqualTo(0.333, Offset.offset(0.001));
		assertThat(sample[1]).isEqualTo(0.333, Offset.offset(0.001));
		assertThat(sample[2]).isEqualTo(0.333, Offset.offset(0.001));

	}
}
