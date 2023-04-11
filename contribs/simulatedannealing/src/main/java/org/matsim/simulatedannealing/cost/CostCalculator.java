/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.simulatedannealing.cost;

/**
 *
 * Calculates the cost/energy of the (current) solution. Should implement the cost function
 * that shall be minimised.
 *
 * As this is highly problem specific, there is no default implementation.
 *
 * @author nkuehnel / MOIA
 */
public interface CostCalculator<T> {
	double calculateCost(T solution);

}
