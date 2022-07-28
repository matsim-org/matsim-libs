package org.matsim.modechoice.constraints;

import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeEstimate;
import org.matsim.modechoice.PlanModel;

import javax.annotation.Nullable;

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
	T getContext(EstimatorContext context, PlanModel model);

	/**
	 * Determine if a combination of modes is valid.
	 *
	 * @param context the context that was provided via {@link #getContext(EstimatorContext, PlanModel)}
	 * @param modes   array of modes. This array must not be stored as it will be re-sued. Entries may contain null for undecided modes.
	 * @return whether this is valid mode combination
	 */
	boolean isValid(T context, String[] modes);

	/**
	 * Determine if a mode can be used at all. This allows to filter many invalid combinations beforehand without the need to search through all of them.
	 *
	 * @param currentModes current trip modes that will be modified
	 * @param mode         the moe in question
	 * @param mask         if not null, mask of trips that will be modified.
	 */
	default boolean isValidMode(T context, String[] currentModes, ModeEstimate mode, @Nullable boolean[] mask) {
		return true;
	}

}
