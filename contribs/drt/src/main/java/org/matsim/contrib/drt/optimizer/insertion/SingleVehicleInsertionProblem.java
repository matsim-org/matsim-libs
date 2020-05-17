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
import java.util.Optional;
import java.util.function.ToDoubleFunction;

import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.DetourDataProvider.DetourDataSet;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

/**
 * @author michalm
 */
public class SingleVehicleInsertionProblem<D> {
	public static SingleVehicleInsertionProblem<PathData> createWithDetourPathProvider(
			DetourDataProvider<PathData> pathDataProvider, InsertionCostCalculator costCalculator) {
		return new SingleVehicleInsertionProblem<>(pathDataProvider, PathData::getTravelTime, costCalculator);
	}

	public static SingleVehicleInsertionProblem<Double> createWithDetourTimeProvider(
			DetourDataProvider<Double> detourTimeProvider, InsertionCostCalculator costCalculator) {
		return new SingleVehicleInsertionProblem<>(detourTimeProvider, Double::doubleValue, costCalculator);
	}

	public static class BestInsertion<D> {
		public final InsertionWithDetourData<D> insertion;
		public final VehicleData.Entry vehicleEntry;
		public final double cost;

		public BestInsertion(InsertionWithDetourData<D> insertion, VehicleData.Entry vehicleEntry, double cost) {
			this.insertion = insertion;
			this.vehicleEntry = vehicleEntry;
			this.cost = cost;
		}
	}

	private final InsertionCostCalculator costCalculator;
	private final DetourDataProvider<D> pathDataProvider;
	private final ToDoubleFunction<D> detourTime;

	SingleVehicleInsertionProblem(DetourDataProvider<D> pathDataProvider, ToDoubleFunction<D> detourTime,
			InsertionCostCalculator costCalculator) {
		this.pathDataProvider = pathDataProvider;
		this.costCalculator = costCalculator;
		this.detourTime = detourTime;
	}

	public Optional<BestInsertion<D>> findBestInsertion(DrtRequest drtRequest, VehicleData.Entry vEntry,
			List<Insertion> insertions) {
		DetourDataSet<D> set = pathDataProvider.getDetourDataSet(drtRequest, vEntry);

		double minCost = InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
		InsertionWithDetourData<D> bestInsertion = null;
		for (Insertion i : insertions) {
			InsertionWithDetourData<D> insertion = set.createInsertionDetourData(i);
			double cost = costCalculator.calculate(drtRequest, vEntry, insertion, detourTime);
			if (cost < minCost) {
				bestInsertion = insertion;
				minCost = cost;
			}
		}

		return minCost == InsertionCostCalculator.INFEASIBLE_SOLUTION_COST ?
				Optional.empty() :
				Optional.of(new BestInsertion<>(bestInsertion, vEntry, minCost));
	}
}
