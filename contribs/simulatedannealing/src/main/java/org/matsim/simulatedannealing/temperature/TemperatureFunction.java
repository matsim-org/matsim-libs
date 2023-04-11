/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.simulatedannealing.temperature;

/**
 *
 * The temperature function calculates the simulated annealing temperature that
 * influences the acceptance probability of a new solution in the default setup.
 *
 * Contains multiple default implementations.
 *
 * @author nkuehnel / MOIA
 */
public interface TemperatureFunction {

	double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
						  double currentCost, double bestCost);

	enum DefaultFunctions implements TemperatureFunction {
		//multiplicative
		exponentialMultiplicative {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return initialTemperature * Math.pow(alpha, iteration);
			}
		},
		logarithmicMultiplicative{
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return initialTemperature / (1 + alpha * Math.log(1 + iteration));
			}
		},
		linearMultiplicative {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return initialTemperature / (1 + alpha * iteration);
			}
		},
		quadraticMultiplicative {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return initialTemperature / (1 + alpha * iteration * iteration);
			}
		},

		//additive
		exponentialAdditive {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return finalTemperature
						+ (initialTemperature - finalTemperature)
						* (1 / (1 + Math.exp((2 * Math.log(initialTemperature - finalTemperature) / cycles) * iteration - 0.5 * cycles)));
			}
		},
		linearAdditive {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return finalTemperature + (initialTemperature - finalTemperature) * ((double) (cycles - iteration) / cycles);
			}
		},
		quadraticAdditive {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return finalTemperature + (initialTemperature - finalTemperature) * Math.pow(((double) (cycles - iteration) / cycles), 2);
			}
		},
		trigonometricAdditive {
			@Override
			public double getTemperature(double alpha, double initialTemperature, double finalTemperature, int cycles, int iteration,
										 double currentCost, double bestCost) {
				return finalTemperature + 0.5 * (initialTemperature - finalTemperature) * (1 + Math.cos((iteration * Math.PI) / cycles));
			}
		}
	}
}
