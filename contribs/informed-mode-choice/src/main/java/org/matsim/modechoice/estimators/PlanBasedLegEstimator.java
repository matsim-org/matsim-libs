package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

/**
 * An estimator that needs to each trip taken during a full day in order to create an accurate estimate.
 * @param <T> mode options
 */
public interface PlanBasedLegEstimator<T extends Enum<?>>{

	/**
	 * Estimate the utility for all legs that uses this mode in {@link PlanModel}.
	 */
	double estimate(EstimatorContext context, String mode, PlanModel plan, T option);


	MinMaxEstimate estimateMinMax(EstimatorContext context, String mode, PlanModel plan, T option, Leg leg);

	// TODO: may provide lower and upper bound estimates ?

	// TODO: distances and departure times for all trips of the day
	// might be useful for some estimates (pt)

}
