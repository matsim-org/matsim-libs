/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedannealing.perturbation;

import simulatedannealing.SimulatedAnnealing;
import simulatedannealing.SimulatedAnnealingListener;

/**
 *
 * Provides a function to create pertubators dynamically during the simulation.
 *
 * @author nkuehnel / MOIA
 */
public interface PerturbatorFactory<T> extends SimulatedAnnealingListener<T> {

	Perturbator<T> createPerturbator(int iteration, double temperature);

	default void solutionAccepted(SimulatedAnnealing.Solution<T> oldSolution, SimulatedAnnealing.Solution<T> newSolution) {}

	default void reset(int iteration){}

}
