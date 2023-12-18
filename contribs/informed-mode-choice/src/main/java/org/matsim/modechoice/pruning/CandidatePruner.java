package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import java.util.Collection;
import java.util.Random;

/**
 * Class used for pruning unpromising candidates, i.e. their score is below the calculated threshold.
 */
public interface CandidatePruner {

	/**
	 * Remove candidates from a given collection. This method is expected to modify the collection directly.
	 * This method of pruning can be used for more complex strategies, but requires that the necesarry amount of candidates is generated first.
	 * The threshold based pruning {@link #planThreshold(PlanModel)} is usually more efficient.
	 */
	default void pruneCandidates(PlanModel model, Collection<PlanCandidate> candidates, Random rnd) {
	}

	/**
	 * Calculate threshold to be applied on the best known plan estimate. Candidates with a larger difference to the best, than this threshold are discarded.
	 * @return positive threshold, if negative it will not be applied
	 */
	double planThreshold(PlanModel planModel);


	/**
	 * Calculate threshold to be applied on a single trip. Modes worse than this threshold on this trip will be discarded.
	 * @return positive threshold, if negative it will not be applied
	 */
	double tripThreshold(PlanModel planModel, int idx);
}
