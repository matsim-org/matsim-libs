package org.matsim.contrib.optimization.simulatedAnnealing;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.optimization.simulatedAnnealing.acceptor.Acceptor;
import org.matsim.contrib.optimization.simulatedAnnealing.cost.CostCalculator;
import org.matsim.contrib.optimization.simulatedAnnealing.perturbation.Perturbator;
import org.matsim.contrib.optimization.simulatedAnnealing.perturbation.PerturbatorFactory;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.OptionalDouble;

/**
 * @author nkuehnel
 * @param <T>
 */
public final class SimulatedAnnealing<T> implements IterationEndsListener, Provider<T> {

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
			return Double.isNaN(cost) ? OptionalDouble.empty(): OptionalDouble.of(cost);
		}
	}

	private static final Logger logger = LogManager.getLogger( SimulatedAnnealing.class ) ;

	private Solution<T> currentSolution;
	private Solution<T> acceptedSolution;
	private Solution<T> bestSolution;
	private final Solution<T> initialSolution;

	private final CostCalculator<T> costCalculator;
	private final Acceptor<T> acceptor;
	private final PerturbatorFactory<T> perturbatorFactory;

	private final SimulatedAnnealingConfigGroup simAnCfg;

	public SimulatedAnnealing(CostCalculator<T> costCalculator, Acceptor<T> acceptor, PerturbatorFactory<T> perturbatorFactory, T initialSolution, SimulatedAnnealingConfigGroup simAnCfg) {
		this.costCalculator = costCalculator;
		this.acceptor = acceptor;
		this.perturbatorFactory = perturbatorFactory;
		this.initialSolution = new Solution<>(initialSolution, Double.POSITIVE_INFINITY);
		this.simAnCfg = simAnCfg;
		this.currentSolution = this.initialSolution;
		this.bestSolution = this.initialSolution;
		this.acceptedSolution = this.initialSolution;

	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if(event.getIteration() % simAnCfg.iterationRatio == 0) {

			currentSolution = new Solution<>(currentSolution.solution, costCalculator.calculateCost(currentSolution.solution));

			if (acceptor.accept(currentSolution, acceptedSolution, bestSolution)) {
				acceptedSolution = currentSolution;
				logger.debug(String.format("Updated accepted {%s} solution with cost of %f in iteration %d", currentSolution.solution.toString(), currentSolution.cost, event.getIteration()));
				if (bestSolution == null || acceptedSolution.cost < bestSolution.cost) {
					bestSolution = acceptedSolution;
					logger.debug(String.format("Updated best solution {%s} with cost of %f in iteration %d", bestSolution.solution.toString(), bestSolution.cost, event.getIteration()));
				}
			}

			Perturbator<T> perturbator = perturbatorFactory.createPerturbator();
			T newSolution = perturbator.perturbate(acceptedSolution.solution);
			currentSolution = new Solution<>(newSolution);
		}
	}


	public T getAcceptedSolution() {
		return acceptedSolution.solution;
	}

	@Override
	public T get() {
		return acceptedSolution.solution;
	}
}
