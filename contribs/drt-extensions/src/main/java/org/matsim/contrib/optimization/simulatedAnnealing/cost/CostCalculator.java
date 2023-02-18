package org.matsim.contrib.optimization.simulatedAnnealing.cost;

public interface CostCalculator<T> {
	double calculateCost(T solution);

}
