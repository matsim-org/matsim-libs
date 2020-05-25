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
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem<PathData> {

	// step 1: initial filtering out feasible insertions
	// FIXME make it more flexible... 40 is way too big for many smaller scenarios, we may also want to reduce 1.5
	private static final int NEAREST_INSERTIONS_AT_END_LIMIT = 40;
	static final double OPTIMISTIC_BEELINE_SPEED_COEFF = 1.5;
	private final InsertionCostCalculator<Double> approximateCostCalculator;
	private final DetourTimesProvider optimisticDetourTimesProvider;

	// step 2: finding best insertion
	private final ForkJoinPool forkJoinPool;
	private final PathDataProvider pathDataProvider;
	private final BestInsertionFinder<PathData> bestInsertionFinder;

	public ParallelMultiVehicleInsertionProblem(PathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer, ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.pathDataProvider = pathDataProvider;
		this.forkJoinPool = forkJoinPool;

		approximateCostCalculator = new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator,
				Double::doubleValue);

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = OPTIMISTIC_BEELINE_SPEED_COEFF * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();

		optimisticDetourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createBeelineTimeEstimator(optimisticBeelineSpeed));

		bestInsertionFinder = new BestInsertionFinder<>(
				new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, PathData::getTravelTime));
	}

	@Override
	public Optional<InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
			Collection<Entry> vEntries) {
		InsertionGenerator insertionGenerator = new InsertionGenerator();
		DetourData<Double> timeData = optimisticDetourTimesProvider.getDetourData(drtRequest);
		KNearestInsertionsAtEndFilter kNearestInsertionsAtEndFilter = new KNearestInsertionsAtEndFilter(
				NEAREST_INSERTIONS_AT_END_LIMIT);

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> vEntries.parallelStream()
				//generate feasible insertions (wrt occupancy limits)
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				//optimistic pre-filtering wrt (admissible cost function using an optimistic beeline speed coefficient)
				.map(timeData::createInsertionWithDetourData)
				.filter(insertion -> approximateCostCalculator.calculate(drtRequest, insertion)
						< InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				//skip insertions at schedule ends (only selected will be added later)
				.filter(kNearestInsertionsAtEndFilter::filter)
				//forget (approximated) detour times
				.map(InsertionWithDetourData::getInsertion)
				.collect(Collectors.toList())).join();
		filteredInsertions.addAll(kNearestInsertionsAtEndFilter.getNearestInsertionsAtEnd());

		DetourData<PathData> pathData = pathDataProvider.getPathData(drtRequest, filteredInsertions);
		//TODO could use a parallel stream within forkJoinPool, however the idea is to have as few filteredInsertions
		// as possible, and then using a parallel stream does not make sense.
		return bestInsertionFinder.findBestInsertion(drtRequest,
				filteredInsertions.stream().map(pathData::createInsertionWithDetourData));
	}
}
