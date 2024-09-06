/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.StopTimeCalculator;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
class ExtensiveInsertionProvider {
	static ExtensiveInsertionProvider create(DrtConfigGroup drtCfg, InsertionCostCalculator insertionCostCalculator,
			ForkJoinPool forkJoinPool, StopTimeCalculator stopTimeCalculator,
			DetourTimeEstimator admissibleTimeEstimator) {
		return new ExtensiveInsertionProvider((ExtensiveInsertionSearchParams) drtCfg.getDrtInsertionSearchParams(),
				insertionCostCalculator, new InsertionGenerator(stopTimeCalculator, admissibleTimeEstimator),
				forkJoinPool);
	}

	private final ExtensiveInsertionSearchParams insertionParams;
	private final InsertionCostCalculator admissibleCostCalculator;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	@VisibleForTesting
	ExtensiveInsertionProvider(ExtensiveInsertionSearchParams insertionParams,
			InsertionCostCalculator admissibleCostCalculator, InsertionGenerator insertionGenerator,
			ForkJoinPool forkJoinPool) {
		this.insertionParams = insertionParams;
		this.admissibleCostCalculator = admissibleCostCalculator;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<InsertionWithDetourData> preFilteredInsertions = forkJoinPool.submit(() -> vehicleEntries.parallelStream()
				//generate feasible insertions (wrt occupancy limits) with admissible detour times
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				//optimistic pre-filtering wrt admissible cost function
				.filter(i -> admissibleCostCalculator.calculate(drtRequest, i.insertion, i.detourTimeInfo)
						< INFEASIBLE_SOLUTION_COST)
				//collect
				.collect(Collectors.toList())).join();

		if (preFilteredInsertions.isEmpty()) {
			return List.of();
		}

		return KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(insertionParams.nearestInsertionsAtEndLimit,
				preFilteredInsertions);
	}
}
