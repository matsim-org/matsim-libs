package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

/**
 * Estimator for a single leg.
 *
 * @param <T> enumeration of possible ownerships
 */
public interface LegEstimator {

	/**
	 * Calculate an estimate of utility íf this mode would be used.
	 * May return {@link Double#NEGATIVE_INFINITY} if the mode is not available.
	 *
	 * @param context person traveling
	 * @param mode    desire mode
	 * @param leg     leg to estimate
	 * @param option  used mode availability
	 * @return Estimated utility
	 */
	double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option);



}
