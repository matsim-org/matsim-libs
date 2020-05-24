/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * Feasibility wrt DetourDataProvider and InsertionCostCalculator
 *
 * @author michalm
 */
public class FeasibleInsertionFilter<D> {
	private final InsertionCostCalculator<D> costCalculator;

	public FeasibleInsertionFilter(InsertionCostCalculator<D> costCalculator) {
		this.costCalculator = costCalculator;
	}

	public boolean filter(DrtRequest drtRequest, InsertionWithDetourData<D> insertion) {
		return costCalculator.calculate(drtRequest, insertion) < InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
	}
}
