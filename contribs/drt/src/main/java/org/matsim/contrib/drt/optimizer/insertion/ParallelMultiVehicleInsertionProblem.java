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
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem<PathData> {

	// FIXME make it more flexible... 40 is way too big for many smaller scenarios
	private static final int NEAREST_INSERTIONS_AT_END_LIMIT = 40;

	private final PrecalculablePathDataProvider pathDataProvider;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;

	// used to prevent filtering out feasible insertions
	static final double OPTIMISTIC_BEELINE_SPEED_COEFF = 1.5;

	private final InsertionGenerator insertionGenerator = new InsertionGenerator();
	private final FeasibleInsertionFilter<Double> feasibleInsertionFilter;
	private final DetourTimesProvider detourTimesProvider;

	public ParallelMultiVehicleInsertionProblem(PrecalculablePathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer, ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.pathDataProvider = pathDataProvider;
		this.forkJoinPool = forkJoinPool;
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer, penaltyCalculator);

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = OPTIMISTIC_BEELINE_SPEED_COEFF * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();

		detourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createBeelineTimeEstimator(optimisticBeelineSpeed));
		feasibleInsertionFilter = FeasibleInsertionFilter.createWithDetourTimes(
				new InsertionCostCalculator(drtCfg, timer, penaltyCalculator));
	}

	@Override
	public Optional<BestInsertion<PathData>> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		DetourDataProvider.DetourData<Double> data = detourTimesProvider.getDetourData(drtRequest);
		KNearestInsertionsAtEndFilter KNearestInsertionsAtEndFilter = new KNearestInsertionsAtEndFilter(
				NEAREST_INSERTIONS_AT_END_LIMIT);

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> vEntries.parallelStream()
				//generate feasible insertions (wrt occupancy limits)
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				//optimistic pre-filtering wrt (admissible cost function using an optimistic beeline speed coefficient)
				.map(data::createInsertionWithDetourData)
				.filter(insertion -> feasibleInsertionFilter.filter(drtRequest, insertion))
				//skip insertions at schedule ends (only selected will be added later)
				.filter(KNearestInsertionsAtEndFilter::filter)
				//forget (approximated) detour times
				.map(InsertionWithDetourData::getInsertion)
				.collect(Collectors.toList())).join();

		filteredInsertions.addAll(KNearestInsertionsAtEndFilter.getNearestInsertionsAtEnd());

		pathDataProvider.precalculatePathData(drtRequest, filteredInsertions);

		return SingleVehicleInsertionProblem.createWithDetourPathProvider(pathDataProvider, insertionCostCalculator)
				.findBestInsertion(drtRequest, filteredInsertions);
	}
}
