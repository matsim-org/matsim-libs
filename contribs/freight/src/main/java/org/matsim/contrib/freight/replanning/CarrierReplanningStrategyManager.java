package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

public class CarrierReplanningStrategyManager {

	private List<CarrierReplanningStrategy> strategies = new ArrayList<CarrierReplanningStrategy>();

	private List<Double> weights = new ArrayList<Double>();

	public void addStrategy(CarrierReplanningStrategy strategy, double weight) {
		strategies.add(strategy);
		weights.add(weight);
	}

	public CarrierReplanningStrategy nextStrategy() {
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
