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

package org.matsim.contrib.drt.optimizer.insertion.selective;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.stops.StopTimeCalculator;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
class SelectiveInsertionProvider {
	public static SelectiveInsertionProvider create(InsertionCostCalculator insertionCostCalculator,
			ForkJoinPool forkJoinPool, StopTimeCalculator stopTimeCalculator,
			DetourTimeEstimator restrictiveDetourTimeEstimator) {
		return new SelectiveInsertionProvider(new BestInsertionFinder(insertionCostCalculator),
				new InsertionGenerator(stopTimeCalculator, restrictiveDetourTimeEstimator), forkJoinPool);
	}

	private final BestInsertionFinder initialInsertionFinder;
	private final InsertionGenerator insertionGenerator;
	private final ForkJoinPool forkJoinPool;

	@VisibleForTesting
	SelectiveInsertionProvider(BestInsertionFinder initialInsertionFinder, InsertionGenerator insertionGenerator,
			ForkJoinPool forkJoinPool) {
		this.initialInsertionFinder = initialInsertionFinder;
		this.insertionGenerator = insertionGenerator;
		this.forkJoinPool = forkJoinPool;
	}

	Optional<InsertionWithDetourData> getInsertion(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		return forkJoinPool.submit(
				// find best insertion given a stream of insertion with time data
				() -> initialInsertionFinder.findBestInsertion(drtRequest,
						//for each vehicle entry
						vehicleEntries.parallelStream()
								//generate feasible insertions (wrt occupancy limits) with restrictive detour times
								.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream()))).join();
	}
}
