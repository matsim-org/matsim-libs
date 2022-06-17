package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

import java.util.List;

/**
 * Estimator for a trip consisting of multiple legs.
 *
 * @param <T> enumeration of possible ownerships
 */
public interface TripEstimator<T extends Enum<?>> {

	/**
	 * Calculate a *minimum* estimate of utility íf this mode would be used. This method will be called with all legs of a trip.
	 * legs not belonging to this mode must not be estimated and are only present as context.
	 *
	 * @param context person traveling
	 * @param mode    desire mode
	 * @param plan    plan model of the day
	 * @param trip    list of used legs
	 * @param option  used mode availability
	 * @return Estimated utility
	 */
	MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, T option);


	/**
	 * Indicate whether an estimate will be uncertain and requires a maximum.
	 */
	default boolean providesMaxEstimate(EstimatorContext context, String mode, T option) {
		return false;
	}


}
