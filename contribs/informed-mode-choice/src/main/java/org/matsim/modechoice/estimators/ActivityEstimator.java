package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.modechoice.EstimatorContext;

/**
 * Estimate the utility for performed activities.
 */
public interface ActivityEstimator {

	/**
	 * Estimate the obtained score of performing an activity.
	 */
	double estimate(EstimatorContext context, double arrivalTime, Activity act);


	final class None implements ActivityEstimator {

		@Override
		public double estimate(EstimatorContext context, double arrivalTime, Activity act) {
			return 0;
		}
	}
}
