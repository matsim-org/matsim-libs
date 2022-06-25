package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

import java.util.List;

/**
 * An estimator that needs to each trip taken during a full day in order to create an accurate estimate.
 * @param <T> mode options
 */
public interface PlanEstimator<T extends Enum<?>> extends TripEstimator<T> {

	/**
	 * Estimate the total utility for all legs that uses this mode in {@link PlanModel}.
	 */
	double estimatePlan(EstimatorContext context, String mode, PlanModel plan, List<List<Leg>> trips, T option);

	// TODO: not yet in use

	// TODO: distances and departure times for all trips of the day
	// might be useful for some estimates (pt)
	// or for certain types of tolls
}
