/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedAnnealing.perturbation;

import simulatedAnnealing.SimulatedAnnealing;

/**
 *
 * A perturbator is responsible for perturbating the accepted solution to find
 * new solutions.
 *
 * NOTE:
 * A strong assumption is that the implementation is responsible for creating
 * a deep copy of the accepted solution before perturbing. Otherwise, the algorithm
 * will not work properly as both accepted and current version pointers in the
 * {@link SimulatedAnnealing} class will point to the same objects.
 *
 * @author nkuehnel / MOIA
 */
public interface Perturbator<T> {

	T perturbate(T current);


}
