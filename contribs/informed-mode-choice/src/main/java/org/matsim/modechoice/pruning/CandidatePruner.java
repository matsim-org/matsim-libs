package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanModel;

/**
 * Class used for pruning unpromising candidates, i.e. their score is below the calculated threshold.
 */
public interface CandidatePruner {

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
