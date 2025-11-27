package org.matsim.modechoice.estimators;

import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.EstimatorContext;

/**
 * This class can be used to estimate additional scores for a trip.
 * These score are added to the existing estimates and might be independent of the mode.
 */
public interface TripScoreEstimator {

	/**
	 * Compute a score estimate for a trip.
	 */
	double estimate(EstimatorContext context, String mainMode, TripStructureUtils.Trip trip);


}
