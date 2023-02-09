package org.matsim.contrib.optimization.simulatedAnnealing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.OptionalDouble;

/**
 * @author nkuehnel
 * @param <T>
 */
public final class SimulatedAnnealing<T> implements IterationEndsListener {

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
	private final Perturbator<T> perturbator;

	public SimulatedAnnealing(CostCalculator<T> costCalculator, Acceptor<T> acceptor, Perturbator<T> perturbator, T initialSolution) {
		this.costCalculator = costCalculator;
		this.acceptor = acceptor;
		this.perturbator = perturbator;
		this.initialSolution = new Solution<>(initialSolution, Double.POSITIVE_INFINITY);
		this.currentSolution = this.initialSolution;
		this.bestSolution = this.initialSolution;
		this.acceptedSolution = this.initialSolution;
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		currentSolution = new Solution<>(currentSolution.solution, costCalculator.calculateCost(currentSolution.solution));

		if (acceptor.accept(currentSolution, acceptedSolution, bestSolution)) {
			acceptedSolution = currentSolution;
			logger.debug(String.format("Current solution accepted with cost of %f", currentSolution.cost));
			if(bestSolution == null || acceptedSolution.cost < bestSolution.cost) {
				bestSolution = acceptedSolution;
				logger.debug(String.format("Updated best %s solution with cost of %f", bestSolution.solution.toString(), bestSolution.cost));
			}
		}

		T newSolution = perturbator.perturbate(acceptedSolution.solution);
		currentSolution = new Solution<>(newSolution);
	}


	public T getAcceptedSolution() {
		return acceptedSolution.solution;
	}
}
