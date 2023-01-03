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

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author michalm
 */
public class BestInsertionFinder {
	public static class InsertionWithCost {
		public final InsertionWithDetourData insertionWithDetourData;
		public final double cost;

		public InsertionWithCost(InsertionWithDetourData insertionWithDetourData, double cost) {
			this.insertionWithDetourData = insertionWithDetourData;
			this.cost = cost;
		}
	}

	public static final Comparator<Insertion> INSERTION_COMPARATOR = //
			Comparator.<Insertion, Id<DvrpVehicle>>comparing(insertion -> insertion.vehicleEntry.vehicle.getId())
					.thenComparingInt(insertion -> insertion.pickup.index)
					.thenComparingInt(insertion -> insertion.dropoff.index);

	public static final Comparator<InsertionWithCost> INSERTION_WITH_COST_COMPARATOR = //
			Comparator.<InsertionWithCost>comparingDouble(insertionWithCost -> insertionWithCost.cost)
					.thenComparing(insertion -> insertion.insertionWithDetourData.insertion, INSERTION_COMPARATOR);

	private final InsertionCostCalculator costCalculator;

	public BestInsertionFinder(InsertionCostCalculator costCalculator) {
		this.costCalculator = costCalculator;
	}

	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
			Stream<InsertionWithDetourData> insertions) {
		return insertions.map(
						i -> new InsertionWithCost(i, costCalculator.calculate(drtRequest, i.insertion, i.detourTimeInfo)))
				.filter(iWithCost -> iWithCost.cost < INFEASIBLE_SOLUTION_COST)
				.min(INSERTION_WITH_COST_COMPARATOR)
				.map(iWithCost -> iWithCost.insertionWithDetourData);
	}
}
