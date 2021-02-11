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
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class ExtensiveInsertionProvider implements InsertionProvider {
	public static ExtensiveInsertionProvider create(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			ForkJoinPool forkJoinPool) {
		var insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getAdmissibleBeelineSpeedFactor(), dvrpTravelTimeMatrix);
		return new ExtensiveInsertionProvider(drtCfg, timer, costCalculationStrategy, restrictiveDetourTimeEstimator,
				forkJoinPool);
	}

	private final ExtensiveInsertionSearchParams insertionParams;
	private final InsertionCostCalculator<Double> admissibleCostCalculator;
	private final DetourTimeEstimator admissibleDetourTimeEstimator;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	public ExtensiveInsertionProvider(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, DetourTimeEstimator admissibleDetourTimeEstimator,
			ForkJoinPool forkJoinPool) {
		this((ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams(),
				new InsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, Double::doubleValue,
						admissibleDetourTimeEstimator), admissibleDetourTimeEstimator, new InsertionGenerator(),
				forkJoinPool);
	}

	@VisibleForTesting
	ExtensiveInsertionProvider(ExtensiveInsertionSearchParams insertionParams,
			InsertionCostCalculator<Double> admissibleCostCalculator, DetourTimeEstimator admissibleDetourTimeEstimator,
			InsertionGenerator insertionGenerator, ForkJoinPool forkJoinPool) {
		this.insertionParams = insertionParams;
		this.admissibleCostCalculator = admissibleCostCalculator;
		this.admissibleDetourTimeEstimator = admissibleDetourTimeEstimator;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		DetourData<Double> admissibleTimeData = DetourData.create(admissibleDetourTimeEstimator, drtRequest);

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<InsertionWithDetourData<Double>> preFilteredInsertions = forkJoinPool.submit(
				() -> vehicleEntries.parallelStream()
						//generate feasible insertions (wrt occupancy limits)
						.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
						//map insertions to insertions with admissible detour times (i.e. admissible beeline speed factor)
						.map(admissibleTimeData::createInsertionWithDetourData)
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
