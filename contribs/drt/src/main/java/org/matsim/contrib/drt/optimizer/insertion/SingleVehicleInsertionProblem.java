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

import java.util.Optional;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

/**
 * @author michalm
 */
public class SingleVehicleInsertionProblem {
	public static class BestInsertion {
		public final Insertion insertion;
		public final VehicleData.Entry vehicleEntry;
		public final double cost;

		public BestInsertion(Insertion insertion, VehicleData.Entry vehicleEntry, double cost) {
			this.insertion = insertion;
			this.vehicleEntry = vehicleEntry;
			this.cost = cost;
		}
	}

	private final InsertionGenerator insertionGenerator;
	private final InsertionCostCalculator costCalculator;

	public SingleVehicleInsertionProblem(PathDataProvider pathDataProvider, InsertionCostCalculator costCalculator) {
		this.insertionGenerator = new InsertionGenerator(pathDataProvider);
		this.costCalculator = costCalculator;
	}

	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		double minCost = InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
		Insertion bestInsertion = null;
		for (Insertion insertion : insertionGenerator.generateInsertions(drtRequest, vEntry)) {
			double cost = costCalculator.calculate(drtRequest, vEntry, insertion);
			if (cost < minCost) {
				bestInsertion = insertion;
				minCost = cost;
			}
		}
		return minCost == InsertionCostCalculator.INFEASIBLE_SOLUTION_COST ? Optional.empty()
				: Optional.of(new BestInsertion(bestInsertion, vEntry, minCost));
	}
}
