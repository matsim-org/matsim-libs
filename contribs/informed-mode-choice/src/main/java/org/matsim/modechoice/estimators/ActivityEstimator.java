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

	/**
	 * Estimate the obtained score for the last and first activity in a plan. This special case can be used for overnight activities.
	 * If not implemented, the normal estimation for a single activity will be used.
	 */
	default double estimateLastAndFirstOfDay(EstimatorContext context, double arrivalTime, Activity last, Activity first) {
		return estimate(context, arrivalTime, last);
	}

	final class None implements ActivityEstimator {

		@Override
		public double estimate(EstimatorContext context, double arrivalTime, Activity act) {
			return 0;
		}
	}
}
