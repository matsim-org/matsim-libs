package org.matsim.modechoice.pruning;

import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.modechoice.PlanModel;

/**
 * Prunes candidates based on the probability calculated using change exp beta formula.
 * <p>
 * E.g. using percentile of 0.9 will remove plans for which the probability of being changed to is less than 10%
 *
 * The smaller p, the more plans are removed.
 *
 *
 * @see org.matsim.core.replanning.selectors.ExpBetaPlanChanger
 */
public class ChangeExpBetaPruner implements CandidatePruner {

	private final double threshold;

	/**
	 * Create pruner with the desired percentile of plans to keep.
	 * @param p percentile larger than 0.01 and 1.
	 */
	public ChangeExpBetaPruner(ScoringConfigGroup config, double p) {

		// because of hard-coded 0.01 in ExpBetaPlanChanger
		if (p <= 0.01 || p >= 1) {
			throw new IllegalArgumentException("p must be larger than 0.01 and smaller than 1");
		}

		// Calculate utility threshold based on desired percentage
		threshold = 2 * Math.log(100 * p) / config.getBrainExpBeta();
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		return threshold;
	}

	@Override
	public double tripThreshold(PlanModel planModel, int idx) {
		return threshold;
	}
}
