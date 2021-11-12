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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch.InsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.InsertionCostCalculatorFactory;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class SelectiveInsertionProvider implements InsertionProvider {
	public static SelectiveInsertionProvider create(DrtConfigGroup drtCfg,
			InsertionCostCalculatorFactory insertionCostCalculatorFactory, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			ForkJoinPool forkJoinPool) {
		var insertionParams = (SelectiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getRestrictiveBeelineSpeedFactor(), dvrpTravelTimeMatrix);
		var restrictiveCostCalculator = insertionCostCalculatorFactory.create(Double::doubleValue,
				restrictiveDetourTimeEstimator);
		return new SelectiveInsertionProvider(restrictiveDetourTimeEstimator, forkJoinPool, restrictiveCostCalculator);
	}

	private final DetourTimeEstimator restrictiveTimeEstimator;
	private final BestInsertionFinder<Double> initialInsertionFinder;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	public SelectiveInsertionProvider(DetourTimeEstimator restrictiveTimeEstimator, ForkJoinPool forkJoinPool,
			InsertionCostCalculator<Double> restrictiveCostCalculator) {
		this(restrictiveTimeEstimator, new BestInsertionFinder<>(restrictiveCostCalculator), new InsertionGenerator(),
				forkJoinPool);
	}

	@VisibleForTesting
	SelectiveInsertionProvider(DetourTimeEstimator restrictiveTimeEstimator,
			BestInsertionFinder<Double> initialInsertionFinder, InsertionGenerator insertionGenerator,
			ForkJoinPool forkJoinPool) {
		this.restrictiveTimeEstimator = restrictiveTimeEstimator;
		this.initialInsertionFinder = initialInsertionFinder;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		DetourTime restrictiveDetourTime = new DetourTime(restrictiveTimeEstimator);

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		Optional<InsertionWithDetourData<Double>> bestInsertion = forkJoinPool.submit(
				// find best insertion given a stream of insertion with time data
				() -> initialInsertionFinder.findBestInsertion(drtRequest,
						//for each vehicle entry
						vehicleEntries.parallelStream()
								//generate feasible insertions (wrt occupancy limits) with restrictive detour times
								.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e,
										restrictiveDetourTime).stream()))).join();

		return bestInsertion.map(InsertionWithDetourData::getInsertion).stream().collect(toList());
	}
}
