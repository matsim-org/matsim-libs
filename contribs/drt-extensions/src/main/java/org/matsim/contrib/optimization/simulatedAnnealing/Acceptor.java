package org.matsim.contrib.optimization.simulatedAnnealing;

public interface Acceptor<T> {

	boolean accept(SimulatedAnnealing.Solution<T> currentSolution,
				   SimulatedAnnealing.Solution<T> acceptedSolution,
				   SimulatedAnnealing.Solution<T> bestSolution
	);
}
