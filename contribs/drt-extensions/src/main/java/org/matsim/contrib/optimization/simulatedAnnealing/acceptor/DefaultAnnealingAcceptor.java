package org.matsim.contrib.optimization.simulatedAnnealing.acceptor;

import org.apache.commons.math3.random.RandomGenerator;
import org.matsim.contrib.optimization.simulatedAnnealing.SimulatedAnnealing;
import org.matsim.contrib.optimization.simulatedAnnealing.SimulatedAnnealingConfigGroup;
import org.matsim.contrib.optimization.simulatedAnnealing.temperature.TemperatureFunction;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.gbl.Gbl;

/**
 * @param <T>
 * @author nkuehnel
 */
public final class DefaultAnnealingAcceptor<T> implements Acceptor<T> {

	private final RandomGenerator random;

	private final IterationCounter iterationCounter;
	private final SimulatedAnnealingConfigGroup simAnCfg;

	private final TemperatureFunction temperatureFunction;

	public DefaultAnnealingAcceptor(IterationCounter iterationCounter, SimulatedAnnealingConfigGroup simAnCfg, TemperatureFunction temperatureFunction) {
		this.iterationCounter = iterationCounter;
		this.simAnCfg = simAnCfg;
		this.temperatureFunction = temperatureFunction;
		this.random = RandomUtils.getLocalGenerator();
	}

	@Override
	public boolean accept(SimulatedAnnealing.Solution<T> currentSolution, SimulatedAnnealing.Solution<T> acceptedSolution,
						  SimulatedAnnealing.Solution<T> bestSolution) {

		Gbl.assertNotNull(currentSolution);
		Gbl.assertNotNull(acceptedSolution);

		Gbl.assertIf(currentSolution.getCost().isPresent());
		Gbl.assertIf(acceptedSolution.getCost().isPresent());

		// If the new solution is better, accept it
		double currentCost = currentSolution.getCost().getAsDouble();
		double acceptedCost = acceptedSolution.getCost().getAsDouble();

		if (currentCost < acceptedCost) {
			return true;
		}

		// If the new solution is worse, calculate an acceptance probability
		double bestCost = bestSolution == null ? Double.POSITIVE_INFINITY : bestSolution.getCost().orElse(Double.POSITIVE_INFINITY);
		double temperature = getCurrentTemperature(currentCost, bestCost);

		double acceptanceProbability = Math.exp(-(simAnCfg.k * (currentCost - acceptedCost) / temperature));

		return random.nextDouble() < acceptanceProbability;
	}


	public double getCurrentTemperature(double currentCost, double bestCost) {

		final double alpha = simAnCfg.alpha;
		final double initialTemperature = simAnCfg.initialTemperature;
		final double finalTemperature = simAnCfg.finalTemperature;

		final int cycles = simAnCfg.nCoolingCycles;
		final int iteration = iterationCounter.getIterationNumber() / (simAnCfg.iterationsPerTemperature * simAnCfg.iterationRatio);


		return temperatureFunction.getTemperature(alpha, initialTemperature, finalTemperature,
				cycles, iteration, currentCost, bestCost);
	}


}
