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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
	private final PrecalculablePathDataProvider pathDataProvider;
	private final DrtConfigGroup drtCfg;
	private final MobsimTimer timer;
	private final InsertionCostCalculator.PenaltyCalculator penaltyCalculator;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;

	public ParallelMultiVehicleInsertionProblem(PrecalculablePathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer, ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.pathDataProvider = pathDataProvider;
		this.drtCfg = drtCfg;
		this.timer = timer;
		this.forkJoinPool = forkJoinPool;
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer, penaltyCalculator);
		this.penaltyCalculator = penaltyCalculator;
	}

	@Override
	public Optional<BestInsertion<PathData>> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		DetourLinksProvider detourLinksProvider = new DetourLinksProvider(drtCfg, timer, drtRequest, penaltyCalculator);
		Map<Entry, List<Insertion>> filteredInsertions = detourLinksProvider.filterInsertions(forkJoinPool, vEntries);
		if (filteredInsertions.isEmpty()) {
			return Optional.empty();
		}

		pathDataProvider.precalculatePathData(drtRequest,
				filteredInsertions.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

		return forkJoinPool.submit(() -> filteredInsertions.entrySet()
				.parallelStream()
				.map(e -> SingleVehicleInsertionProblem.createWithDetourPathProvider(pathDataProvider,
						insertionCostCalculator).findBestInsertion(drtRequest, e.getValue()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.min(Comparator.comparingDouble(i -> i.cost))).join();
	}
}
