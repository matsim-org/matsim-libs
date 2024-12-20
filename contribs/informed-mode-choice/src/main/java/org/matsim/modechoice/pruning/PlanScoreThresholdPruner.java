package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanModel;

/**
 * Removes plans by a fixed utility threshold.
 */
public class PlanScoreThresholdPruner implements CandidatePruner {

	private final double threshold;

	/**
	 * Threshold to be applied on the best known plan estimate. Candidates with a larger difference to the best, than this threshold are discarded.
	 */
	public PlanScoreThresholdPruner(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		return threshold;
	}
}
