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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ExtensiveInsertionSearch implements DrtInsertionSearch<PathData> {
	private final ExtensiveInsertionSearchParams insertionParams;

	// step 1: initial filtering out feasible insertions
	private final InsertionCostCalculator<Double> admissibleCostCalculator;
	private final DetourTimesProvider admissibleDetourTimesProvider;

	// step 2: finding best insertion
	private final ForkJoinPool forkJoinPool;
	private final DetourPathCalculator detourPathCalculator;
	private final BestInsertionFinder<PathData> bestInsertionFinder;

	public ExtensiveInsertionSearch(DetourPathCalculator detourPathCalculator, DrtConfigGroup drtCfg, MobsimTimer timer,
			ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator,
			DvrpTravelTimeMatrix dvrpTravelTimeMatrix) {
		this.detourPathCalculator = detourPathCalculator;
		this.forkJoinPool = forkJoinPool;

		insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		admissibleCostCalculator = new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, Double::doubleValue);

		admissibleDetourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(insertionParams.getAdmissibleBeelineSpeedFactor(),
						dvrpTravelTimeMatrix));

		bestInsertionFinder = new BestInsertionFinder<>(
				new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, PathData::getTravelTime));
	}

	@Override
	public Optional<InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
			Collection<Entry> vEntries) {
		InsertionGenerator insertionGenerator = new InsertionGenerator();
		DetourData<Double> admissibleTimeData = admissibleDetourTimesProvider.getDetourData(drtRequest);
		KNearestInsertionsAtEndFilter kNearestInsertionsAtEndFilter = new KNearestInsertionsAtEndFilter(
				insertionParams.getNearestInsertionsAtEndLimit(), insertionParams.getAdmissibleBeelineSpeedFactor());

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> vEntries.parallelStream()
				//generate feasible insertions (wrt occupancy limits)
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				//map insertions to insertions with admissible detour times (i.e. admissible beeline speed factor)
				.map(admissibleTimeData::createInsertionWithDetourData)
				//optimistic pre-filtering wrt admissible cost function
				.filter(insertion -> admissibleCostCalculator.calculate(drtRequest, insertion)
						< InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				//skip insertions at schedule ends (a subset of most promising "insertionsAtEnd" will be added later)
				.filter(kNearestInsertionsAtEndFilter::filter)
				//forget (admissible) detour times
				.map(InsertionWithDetourData::getInsertion).collect(Collectors.toList())).join();
		filteredInsertions.addAll(kNearestInsertionsAtEndFilter.getNearestInsertionsAtEnd());

		DetourData<PathData> pathData = detourPathCalculator.calculatePaths(drtRequest, filteredInsertions);
		//TODO could use a parallel stream within forkJoinPool, however the idea is to have as few filteredInsertions
		// as possible, and then using a parallel stream does not make sense.
		return bestInsertionFinder.findBestInsertion(drtRequest,
				filteredInsertions.stream().map(pathData::createInsertionWithDetourData));
	}
}
