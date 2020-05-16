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

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.DetourDataProvider.DetourDataSet;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author michalm
 */
public class SingleVehicleInsertionFilter {
	private final DetourTimesProvider detourTimesProvider;
	private final InsertionCostCalculator costCalculator;

	public SingleVehicleInsertionFilter(DetourTimesProvider detourTimesProvider,
			InsertionCostCalculator costCalculator) {
		this.detourTimesProvider = detourTimesProvider;
		this.costCalculator = costCalculator;
	}

	public List<InsertionWithDetourData<Double>> findFeasibleInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry,
			List<Insertion> insertions) {
		DetourDataSet<Double> set = detourTimesProvider.getDetourDataSet(drtRequest, vEntry);
		int stopCount = vEntry.stops.size();

		return insertions.stream()
				.map(set::createInsertionDetourData)
				.filter(iWithDetourTimes -> costCalculator.calculate(drtRequest, vEntry, iWithDetourTimes,
						Double::doubleValue) < InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				.collect(Collectors.toList());
	}
}
