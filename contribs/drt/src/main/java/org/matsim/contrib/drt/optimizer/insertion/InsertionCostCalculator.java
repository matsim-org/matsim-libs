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
 * @author michalm
 */
public interface InsertionCostCalculator<D> {

	interface InsertionCostCalculatorFactory {
		<D> InsertionCostCalculator<D> create(ToDoubleFunction<D> detourTime,
				DetourTimeEstimator replacedDriveTimeEstimator);
	}

	double INFEASIBLE_SOLUTION_COST = Double.POSITIVE_INFINITY;

	double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion);
}
