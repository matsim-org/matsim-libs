package org.matsim.modechoice.constraints;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

/**
 * Interface to determine whether a solution is valid.
 *
 * @param <T> holder for pre computations
 */
public interface TripConstraint<T> {

	/**
	 * This allows to store pre computations for a plan. {@link #isValid(Object, String[])} will be potentially called a lot of times
	 * with various modes and given this context.
	 */
	T getContext(EstimatorContext context, PlanModel model, Plan plan);

	/**
	 * Determine if a combination of modes is valid.
	 *
	 * @param context the context that was provided via {@link #getContext(EstimatorContext, PlanModel, Plan)}
	 * @param modes   array of modes. This array must not be stored as it will be re-sued. Entries may contain null for undecided modes.
	 * @return whether this is valid mode combination
	 */
	boolean isValid(T context, String[] modes);

}
