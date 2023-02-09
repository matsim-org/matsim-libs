package org.matsim.contrib.optimization.simulatedAnnealing;

public interface CostCalculator<T> {


	double calculateCost(T solution);

}
