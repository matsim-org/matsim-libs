package org.matsim.core.replanning.choosers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ForceInnovationStrategyChooserTest {

	private static final int N = 10000;
	private static final int ITER = 10;

	/**
	 * Shows how numbers are selected and permuted every iteration.
	 */
	@Test
	void permutation() {

		List<Integer> collected = new ArrayList<>();

		int lastPerm = 0;

		for (int iter = 0; iter < 1000; iter++) {

			List<Integer> selected = new ArrayList<>();

			int perm = (iter / ITER) % 16;

			if (perm != lastPerm) {

				lastPerm = perm;

				collected.sort(Integer::compareTo);

				// test that every person has been selected
				assertEquals(N, collected.size());

				collected = new ArrayList<>();
			}


			for (int i = 0; i < N; i++) {

				int idx = (ForceInnovationStrategyChooser.hash(i) >>> perm) % ITER;
				int mod = iter % ITER;
				if ( idx == mod || -idx == mod)
					selected.add(i);

			}

			double size = N / (double) ITER;

			// allow % of deviation
			assertEquals(size, selected.size(), size * 0.1);

			collected.addAll(selected);
			//System.out.println("Iteration: " + iter + ", selection: " + selected.size());
		}
	}
}