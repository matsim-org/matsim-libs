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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch.InsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.InsertionCostCalculatorFactory;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class ExtensiveInsertionProvider implements InsertionProvider {
	public static ExtensiveInsertionProvider create(DrtConfigGroup drtCfg,
			InsertionCostCalculatorFactory insertionCostCalculatorFactory, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			TravelTime travelTime, ForkJoinPool forkJoinPool) {
		var insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var admissibleTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getAdmissibleBeelineSpeedFactor(), dvrpTravelTimeMatrix, travelTime);
		var admissibleCostCalculator = insertionCostCalculatorFactory.create(Double::doubleValue,
				admissibleTimeEstimator);
		return new ExtensiveInsertionProvider(drtCfg, admissibleTimeEstimator, forkJoinPool, admissibleCostCalculator);
	}

	private final ExtensiveInsertionSearchParams insertionParams;
	private final InsertionCostCalculator<Double> admissibleCostCalculator;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	public ExtensiveInsertionProvider(DrtConfigGroup drtCfg, DetourTimeEstimator admissibleTimeEstimator,
			ForkJoinPool forkJoinPool, InsertionCostCalculator<Double> admissibleCostCalculator) {
		this((ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams(), admissibleCostCalculator,
				new InsertionGenerator(admissibleTimeEstimator), forkJoinPool);
	}

	@VisibleForTesting
	ExtensiveInsertionProvider(ExtensiveInsertionSearchParams insertionParams,
			InsertionCostCalculator<Double> admissibleCostCalculator, InsertionGenerator insertionGenerator,
			ForkJoinPool forkJoinPool) {
		this.insertionParams = insertionParams;
		this.admissibleCostCalculator = admissibleCostCalculator;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<InsertionWithDetourData<Double>> preFilteredInsertions = forkJoinPool.submit(
				() -> vehicleEntries.parallelStream()
						//generate feasible insertions (wrt occupancy limits) with admissible detour times
						.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
						//optimistic pre-filtering wrt admissible cost function
						.filter(insertion -> admissibleCostCalculator.calculate(drtRequest, insertion)
								< INFEASIBLE_SOLUTION_COST)
						//collect
						.collect(Collectors.toList())).join();

		if (preFilteredInsertions.isEmpty()) {
			return List.of();
		}

		return KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(insertionParams.getNearestInsertionsAtEndLimit(),
				insertionParams.getAdmissibleBeelineSpeedFactor(), preFilteredInsertions);
	}
}
