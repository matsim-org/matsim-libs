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
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class SelectiveInsertionProvider implements InsertionProvider {
	public static SelectiveInsertionProvider create(DrtConfigGroup drtCfg,
			InsertionCostCalculator insertionCostCalculator, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			TravelTime travelTime, ForkJoinPool forkJoinPool) {
		var insertionParams = (SelectiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getRestrictiveBeelineSpeedFactor(), dvrpTravelTimeMatrix, travelTime);
		return new SelectiveInsertionProvider(drtCfg, restrictiveDetourTimeEstimator, forkJoinPool,
				insertionCostCalculator);
	}

	private final BestInsertionFinder initialInsertionFinder;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	public SelectiveInsertionProvider(DrtConfigGroup drtCfg, DetourTimeEstimator restrictiveTimeEstimator,
			ForkJoinPool forkJoinPool, InsertionCostCalculator restrictiveCostCalculator) {
		this(new BestInsertionFinder(restrictiveCostCalculator),
				new InsertionGenerator(drtCfg.getStopDuration(), restrictiveTimeEstimator), forkJoinPool);
	}

	@VisibleForTesting
	SelectiveInsertionProvider(BestInsertionFinder initialInsertionFinder, InsertionGenerator insertionGenerator,
			ForkJoinPool forkJoinPool) {
		this.initialInsertionFinder = initialInsertionFinder;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		Optional<InsertionWithDetourData> bestInsertion = forkJoinPool.submit(
				// find best insertion given a stream of insertion with time data
				() -> initialInsertionFinder.findBestInsertion(drtRequest,
						//for each vehicle entry
						vehicleEntries.parallelStream()
								//generate feasible insertions (wrt occupancy limits) with restrictive detour times
								.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream()))).join();

		return bestInsertion.map(doubleInsertionWithDetourData -> doubleInsertionWithDetourData.insertion)
				.stream()
				.collect(toList());
	}
}
