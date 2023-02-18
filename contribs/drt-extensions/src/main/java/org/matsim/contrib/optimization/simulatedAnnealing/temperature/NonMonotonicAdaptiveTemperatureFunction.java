package org.matsim.contrib.optimization.simulatedAnnealing.temperature;

/**
 * @author nkuehnel
 *
 * based on Locatelli, M. Simulated Annealing Algorithms for Continuous Global Optimization: Convergence Conditions.
 * Journal of Optimization Theory and Applications 104, 121–133 (2000). <a href="https://doi.org/10.1023/A:1004680806815">DOI</a>
 */
public class NonMonotonicAdaptiveTemperatureFunction implements TemperatureFunction {


	private final TemperatureFunction baseTemperatureFunction;

	public NonMonotonicAdaptiveTemperatureFunction(TemperatureFunction baseTemperatureFunction) {
		this.baseTemperatureFunction = baseTemperatureFunction;
	}

	@Override
	public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration, double currentCost, double bestCost) {
		double tK = baseTemperatureFunction.getTemperature(alpha, initialTemperature, finalTemperature, cycles, iteration, currentCost, bestCost);
		double adaptiveFactor = 1 + (currentCost - bestCost) / currentCost;
		return tK * adaptiveFactor;
	}
}
