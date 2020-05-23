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

import java.util.function.ToDoubleFunction;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * Feasibility wrt DetourDataProvider and InsertionCostCalculator
 *
 * @author michalm
 */
public class FeasibleInsertionFilter<D> {
	public static FeasibleInsertionFilter<Double> createWithDetourTimes(InsertionCostCalculator costCalculator) {
		return new FeasibleInsertionFilter<>(costCalculator, Double::doubleValue);
	}

	private final InsertionCostCalculator costCalculator;
	private final ToDoubleFunction<D> detourTime;

	public FeasibleInsertionFilter(InsertionCostCalculator costCalculator, ToDoubleFunction<D> detourTime) {
		this.costCalculator = costCalculator;
		this.detourTime = detourTime;
	}

	public boolean filter(DrtRequest drtRequest, InsertionWithDetourData<D> insertion) {
		return costCalculator.calculate(drtRequest, insertion, detourTime)
				< InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
	}
}
