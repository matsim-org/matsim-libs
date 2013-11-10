package org.matsim.contrib.freight.replanning;

import org.matsim.core.api.internal.MatsimManager;

public interface CarrierReplanningStrategyManagerI extends MatsimManager {

	/**
	 * Adds a strategy and its probability.
	 * 
	 * <p>Sum of weights should be 1.0.
	 * @param strategy
	 * @param weight
	 * @throws IllegalStateException if weightSum > 1.0 or weightSum < 0.0
	 */
	public abstract void addStrategy(CarrierReplanningStrategy strategy, double weight);

	/**
	 * Retrieves a strategy from the set strategy based on a random number.
	 * @param iteration TODO
	 * 
	 * @return CarrierReplanningStrategy
	 * @throws IllegalStateException if sum of weights != 1.0 or if no strategy found.
	 */
	public abstract CarrierReplanningStrategy nextStrategy(int iteration);

}