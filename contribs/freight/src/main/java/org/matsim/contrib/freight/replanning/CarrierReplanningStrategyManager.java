package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Manager managing strategies and their probabilities to be chosen.
 * 
 * @author sschroeder
 * 
 *
 */
public class CarrierReplanningStrategyManager implements MatsimManager {

	private List<CarrierReplanningStrategy> strategies = new ArrayList<CarrierReplanningStrategy>();

	private List<Double> weights = new ArrayList<Double>();

	private double weightSum = 0.0;
	
	/**
	 * Adds a strategy and its probability.
	 * 
	 * <p>Sum of weights should be 1.0.
	 * @param strategy
	 * @param weight
	 * @throws IllegalStateException if weightSum > 1.0 or weightSum < 0.0
	 */
	public void addStrategy(CarrierReplanningStrategy strategy, double weight) {
		strategies.add(strategy);
		weights.add(weight);
		weightSum += weight;
		if(weightSum > 1.0 || weightSum < 0.0) throw new IllegalStateException("sum of weight cannot be smaller than 0.0 or higher than 1.0");
	}

	/**
	 * Retrieves a strategy from the set strategy based on a random number.
	 * 
	 * @return CarrierReplanningStrategy
	 * @throws IllegalStateException if sum of weights != 1.0 or if no strategy found.
	 */
	public CarrierReplanningStrategy nextStrategy(int iteration) {
		if(weightSum != 1.0) throw new IllegalStateException("sum of strategy weights has to be 1.0");
		double randValue = MatsimRandom.getRandom().nextDouble();
		double sumOfWeights = 0.0;
		for (int i = 0; i < strategies.size(); i++) {
			sumOfWeights += weights.get(i);
			if (randValue <= sumOfWeights) {
				return strategies.get(i);
			}
		}
		throw new IllegalStateException("no strat found");
	}
}
