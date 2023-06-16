/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.contrib.simulatedannealing.acceptor.Acceptor;
import org.matsim.contrib.simulatedannealing.cost.CostCalculator;
import org.matsim.contrib.simulatedannealing.perturbation.Perturbator;
import org.matsim.contrib.simulatedannealing.perturbation.PerturbatorFactory;
import org.matsim.contrib.simulatedannealing.temperature.TemperatureFunction;

import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 *
 * Core class of the simulated annealing algorithm. Schedules all the necessary steps and keeps track of
 * the solutions.
 *
 * @author nkuehnel / MOIA
 */
public final class SimulatedAnnealing<T> implements Provider<T>, BeforeMobsimListener, AfterMobsimListener {

	private static final Logger logger = LogManager.getLogger(SimulatedAnnealing.class);


	public final static class Solution<T> {

		private final double cost;
		private final T solution;

		public Solution(T solution) {
			this.solution = solution;
			this.cost = Double.NaN;
		}

		public Solution(T solution, double cost) {
			this.solution = solution;
			this.cost = cost;
		}

		public OptionalDouble getCost() {
			return Double.isNaN(cost) ? OptionalDouble.empty() : OptionalDouble.of(cost);
		}
		public T get() {
			return solution;
		}
	}

	record SimulatedAnnealingIteration<T>(Solution<T> best,
										  Solution<T> accepted, Solution<T> current, double temperature){};

	private Solution<T> currentSolution;
	private Solution<T> acceptedSolution;
	private Solution<T> bestSolution;
	private final CostCalculator<T> costCalculator;
	private final Acceptor<T> acceptor;
	private final PerturbatorFactory<T> perturbatorFactory;

	private final TemperatureFunction temperatureFunction;

	private int startIteration = 0;


	private final SimulatedAnnealingConfigGroup simAnCfg;

	private int iterationsWithoutImprovement = 0;

	private double currentTemperature;

	private SimulatedAnnealingIteration<T> currentState;

	private final List<SimulatedAnnealingListener<T>> listeners = new ArrayList<>();

	public SimulatedAnnealing(CostCalculator<T> costCalculator, Acceptor<T> acceptor, PerturbatorFactory<T> perturbatorFactory,
							  T initialSolution, TemperatureFunction temperatureFunction, SimulatedAnnealingConfigGroup simAnCfg) {
		this.costCalculator = costCalculator;
		this.acceptor = acceptor;
		this.perturbatorFactory = perturbatorFactory;
		this.temperatureFunction = temperatureFunction;
		this.simAnCfg = simAnCfg;
		this.currentSolution = new Solution<>(initialSolution, Double.POSITIVE_INFINITY);;
		this.bestSolution = currentSolution;
		this.acceptedSolution = currentSolution;
		this.currentTemperature = simAnCfg.initialTemperature;
		listeners.add(perturbatorFactory);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iteration = event.getIteration();
		if(simAnCfg.firstIteration >= event.getIteration()) {
			return;
		}
		if (simAnCfg.lastIteration >= iteration && simAnCfg.lastIteration > iteration) {
			if(iteration % simAnCfg.iterationRatio == 0) {
				Perturbator<T> perturbator = perturbatorFactory.createPerturbator(iteration, currentTemperature);
				T newSolution = perturbator.perturbate(acceptedSolution.solution);
				currentSolution = new Solution<>(newSolution);
			}
		} else if(simAnCfg.lastIteration < iteration) {
			currentSolution = bestSolution;
			acceptedSolution = bestSolution;
			currentState = new SimulatedAnnealingIteration<>(bestSolution, acceptedSolution, currentSolution, currentTemperature);
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		int iteration = event.getIteration();

		if(simAnCfg.firstIteration > event.getIteration()) {
			currentState = new SimulatedAnnealingIteration<>(bestSolution, acceptedSolution, currentSolution, currentTemperature);
			return;
		}

		if (simAnCfg.lastIteration >= iteration) {
			double cost = costCalculator.calculateCost(currentSolution.solution);
			currentSolution = new Solution<>(currentSolution.solution, cost);
			calcCurrentTemperature(currentSolution.getCost().orElse(Double.POSITIVE_INFINITY),
					bestSolution.getCost().orElse(Double.POSITIVE_INFINITY), iteration);

			if (acceptor.accept(currentSolution, acceptedSolution, currentTemperature)) {
				Solution<T> oldSolution = acceptedSolution;
				acceptedSolution = currentSolution;
				logger.debug(String.format("Updated accepted {%s} solution with cost of %f in iteration %d",
						currentSolution.solution.toString(), currentSolution.cost, iteration));
				if (bestSolution == null || acceptedSolution.cost < bestSolution.cost) {
					bestSolution = acceptedSolution;
					logger.debug(String.format("Updated best solution {%s} with cost of %f in iteration %d",
							bestSolution.solution.toString(), bestSolution.cost, iteration));
					if (simAnCfg.resetUponBestSolution && simAnCfg.lastResetIteration > iteration){
						reset(iteration, false);
						logger.debug("Reset of acceptor to current iteration");
					}
					iterationsWithoutImprovement = 0;
				} else {
					iterationsWithoutImprovement++;
					if(iterationsWithoutImprovement >= simAnCfg.deadEndIterationReset
							&& simAnCfg.lastResetIteration > iteration) {
						reset(iteration, false);
					}
				}
				for (SimulatedAnnealingListener<T> listener : listeners) {
					listener.solutionAccepted(oldSolution, acceptedSolution);
				}
			} else {
				iterationsWithoutImprovement++;
				if(iterationsWithoutImprovement >= simAnCfg.deadEndIterationReset
						&& simAnCfg.lastResetIteration > iteration) {
					reset(iteration, false);
				}
			}

			if(simAnCfg.lastResetIteration == iteration) {
				reset(iteration, true);
			}

			currentState = new SimulatedAnnealingIteration<>(bestSolution, acceptedSolution, currentSolution, currentTemperature);
		}
	}

	private void reset(int iteration, boolean last) {
		if(last) {
			acceptedSolution = bestSolution;
			return;
		}
		switch (simAnCfg.resetOption) {
			case temperatureOnly -> startIteration = iteration;
			case solutionOnly -> acceptedSolution = new Solution<>(bestSolution.solution, Double.POSITIVE_INFINITY);
			case temperatureAndSolution -> {
				startIteration = iteration;
				acceptedSolution = new Solution<>(bestSolution.solution, Double.POSITIVE_INFINITY);

			}
		}
		for (SimulatedAnnealingListener<T> listener : listeners) {
			listener.reset(iteration);
		}
		iterationsWithoutImprovement = 0;
	}

	private void calcCurrentTemperature(double currentCost, double bestCost, int currentIteration) {

		final double alpha = simAnCfg.alpha;
		final double initialTemperature = simAnCfg.initialTemperature;
		final double finalTemperature = simAnCfg.finalTemperature;

		final int cycles = simAnCfg.nCoolingCycles;
		final int iteration = (currentIteration - startIteration) / (simAnCfg.iterationsPerTemperature * simAnCfg.iterationRatio);

		currentTemperature =  temperatureFunction.getTemperature(alpha, initialTemperature, finalTemperature,
				cycles, iteration, currentCost, bestCost);
	}

	@Override
	public T get() {
		return currentSolution.solution;
	}

	public Optional<SimulatedAnnealingIteration<T>> getCurrentState() {
		return Optional.ofNullable(currentState);
	}

	public void addListener(SimulatedAnnealingListener<T> listener) {
		listeners.add(listener);
	}

	public void removeListener(SimulatedAnnealingListener<T> listener) {
		listeners.remove(listener);
	}
}
