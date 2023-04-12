/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing.acceptor;

import org.matsim.contrib.simulatedannealing.SimulatedAnnealing;

/**
 *
 * The acceptor decides whether the current solution becomes the new accepted solution, normally
 * based on the cost or energy differences between current and accepted solution and current temperature.
 *
 * @author nkuehnel / MOIA
 */
public interface Acceptor<T> {

	boolean accept(SimulatedAnnealing.Solution<T> currentSolution,
				   SimulatedAnnealing.Solution<T> acceptedSolution,
				   double temperature
	);
}
