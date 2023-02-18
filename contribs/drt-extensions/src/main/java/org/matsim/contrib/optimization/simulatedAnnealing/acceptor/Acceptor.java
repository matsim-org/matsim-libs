package org.matsim.contrib.optimization.simulatedAnnealing.acceptor;

import org.matsim.contrib.optimization.simulatedAnnealing.SimulatedAnnealing;

public interface Acceptor<T> {

	boolean accept(SimulatedAnnealing.Solution<T> currentSolution,
				   SimulatedAnnealing.Solution<T> acceptedSolution,
				   SimulatedAnnealing.Solution<T> bestSolution
	);
}
