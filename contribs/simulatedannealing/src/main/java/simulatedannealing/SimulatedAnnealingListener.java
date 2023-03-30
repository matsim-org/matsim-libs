/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedannealing;

/**
 * @author nkuehnel / MOIA
 */
public interface SimulatedAnnealingListener<T> {

	void solutionAccepted(SimulatedAnnealing.Solution<T> oldSolution, SimulatedAnnealing.Solution<T> newSolution);

	void reset(int iteration);


}

