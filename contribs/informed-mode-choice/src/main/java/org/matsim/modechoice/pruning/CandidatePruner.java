package org.matsim.modechoice.pruning;

import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import java.util.List;
import java.util.Random;

/**
 * Class used for pruning unpromising candidates, i.e. their score is below the calculated threshold.
 * All methods of implementations must be thread safe! They will be invoked in parallel during planning!
 * <p>
 * Implementations can also implement {@link ControlerListener} to get notified simulation progress.
 */
public interface CandidatePruner {

	/**
	 * Remove candidates from a given collection. This method is expected to modify the collection directly.
	 * This method of pruning can be used for more complex strategies, but requires that the necesarry amount of candidates is generated first.
	 * The threshold based pruning {@link #planThreshold(PlanModel)} is usually more efficient.
	 */
	default void pruneCandidates(PlanModel model, List<PlanCandidate> candidates, Random rnd) {
	}

	/**
	 * Called when a candidate was selected.
	 *
	 * @param model     the model used for planning
	 * @param candidate the selected candidate
	 */
	default void onSelectCandidate(PlanModel model, PlanCandidate candidate) {
	}

	/**
	 * Calculate threshold to be applied on the best known plan estimate. Candidates with a larger difference to the best, than this threshold are discarded.
	 *
	 * @return positive threshold, if negative it will not be applied
	 */
	default double planThreshold(PlanModel planModel) {
		return -1;
	}

	/**
	 * Calculate threshold to be applied on a single trip. Modes worse than this threshold on this trip will be discarded.
	 *
	 * @return positive threshold, if negative it will not be applied
	 */
	default double tripThreshold(PlanModel planModel, int idx) {
		return planThreshold(planModel);
	}
}
