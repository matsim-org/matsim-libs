package org.matsim.core.replanning.choosers;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ForceInnovationStrategyChooserTest {

	private static final int N = 10000;
	private static final int ITER = 10;

	/**
	 * Shows how numbers are selected and permuted every iteration.
	 */
	@Test
	public void permutation() {

		List<Integer> collected = new ArrayList<>();
		int lastPerm = -1;

		for (int iter = 0; iter < 500; iter++) {

			List<Integer> selected = new ArrayList<>();

			int perm = (iter / ITER) % 32;

			if (perm != lastPerm) {

				lastPerm = perm;

				collected.sort(Integer::compareTo);
				System.out.println(collected.size());

				collected = new ArrayList<>();
			}


			for (int i = 0; i < N; i++) {

				int idx = (ForceInnovationStrategyChooser.hash(i) >>> perm) % ITER;
				int mod = iter % ITER;
				if ( idx == mod || -idx == mod)
					selected.add(i);

			}

			collected.addAll(selected);
			//System.out.println("Iteration: " + iter + ", selection: " + selected.size());
		}
	}
}