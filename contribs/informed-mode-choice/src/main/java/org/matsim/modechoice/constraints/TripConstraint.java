package org.matsim.modechoice.constraints;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.EstimatorContext;

public interface TripConstraint<T> {

	/**
	 * This allows to store pre computations for a plan. {@link #filter(Object, String[])} will be potentially called a lot of times
	 * with various modes and given this context.
	 */
	T getContext(EstimatorContext context, Plan plan);

	/**
	 * Determine if a combination of modes is valid.
	 *
	 * @param context the context that was provided via {@link #getContext(EstimatorContext, Plan)}
	 * @param modes   array of modes. This array must not be stored as it will be re-sued.
	 * @return whether this is valid mode combination
	 */
	boolean filter(T context, String[] modes);

}
