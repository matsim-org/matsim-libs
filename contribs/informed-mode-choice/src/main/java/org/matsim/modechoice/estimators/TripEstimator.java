package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.PlanModel;

import java.util.List;

/**
 * Estimator for a trip consisting of multiple legs. This class can also be used to implement a minimum and maximum estimate if
 * the final estimate can not be given until the whole plan is known.
 * This is usually the case with zone based fare systems or pt pricing based on the distance of the entire day.
 *
 */
public interface TripEstimator {

	/**
	 * Calculate an estimate of utility íf this mode would be used. This method will be called with all legs of a trip.
	 * legs not belonging to this mode must not be estimated and are only present as context.
	 *
	 * @param context person traveling
	 * @param mode    desire mode
	 * @param plan    plan model of the day
	 * @param trip    list of used legs
	 * @param option  used mode availability
	 * @return Estimated utility
	 */
	MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option);

	/**
	 * Provide an estimate for the whole plan. This function must only estimate the cost by using its mode.
	 * @param context person traveling
	 * @param mode desired mode
	 * @param modes all modes used throughout the day
	 * @param plan plan model of the day {@link PlanModel#getLegs(String, int)}
	 * @param option mode availability
	 * @return Estimated utility
	 */
	default double estimatePlan(EstimatorContext context, String mode, String[] modes, PlanModel plan, ModeAvailability option) {
		throw new UnsupportedOperationException("providesMinEstimate returned true, but estimate function for the whole plan is not implemented yet.");
	}

	/**
	 * Indicate whether an estimate will be uncertain, so that it requires an additional minimum estimation.
	 */
	default boolean providesMinEstimate(EstimatorContext context, String mode, ModeAvailability option) {
		return false;
	}

}
