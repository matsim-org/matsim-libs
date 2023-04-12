/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing.temperature;

/**
 *
 * based on:
 * Locatelli, M. Simulated Annealing Algorithms for Continuous Global Optimization: Convergence Conditions.
 * Journal of Optimization Theory and Applications 104, 121–133 (2000).
 * <a href="https://doi.org/10.1023/A:1004680806815">DOI</a>
 *
 *  @author nkuehnel / MOIA
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
