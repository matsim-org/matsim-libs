package org.matsim.modechoice.estimators;

import com.google.inject.Inject;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.PseudoRandomScorer;
import org.matsim.modechoice.EstimatorContext;

/**
 * Provides pseudo random score from {@link PseudoRandomScorer} to the estimator.
 */
public class PseudoRandomTripScoreEstimator implements TripScoreEstimator {

	private final PseudoRandomScorer scorer;

	@Inject
	public PseudoRandomTripScoreEstimator(PseudoRandomScorer scorer) {
		this.scorer = scorer;
	}

	@Override
	public double estimate(EstimatorContext context, String mainMode, TripStructureUtils.Trip trip) {
		return scorer.scoreTrip(context.person.getId(), mainMode, trip);
	}
}
