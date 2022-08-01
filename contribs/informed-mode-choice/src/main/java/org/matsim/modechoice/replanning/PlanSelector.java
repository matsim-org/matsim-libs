package org.matsim.modechoice.replanning;

import org.matsim.modechoice.PlanCandidate;

import java.util.List;

/**
 * Specification of a selector for plans.
 */
public interface PlanSelector extends Selector<PlanCandidate> {

	/**
	 * Sample n times and return relative frequency of how often a candidate was selected.
	 */
	default double[] sample(int n, List<PlanCandidate> candidates) {

		double[] res = new double[candidates.size()];

		for (int i = 0; i < n; i++) {
			res[candidates.indexOf(select(candidates))]++;
		}

		for (int i = 0; i < res.length; i++) {
			res[i] /= n;
		}

		return res;
	}

}
