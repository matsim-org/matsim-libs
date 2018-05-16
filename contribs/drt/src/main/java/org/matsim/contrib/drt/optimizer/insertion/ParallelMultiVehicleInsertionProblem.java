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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {
	private final PrecalculablePathDataProvider pathDataProvider;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;
	private final InsertionGenerator insertionGenerator = new InsertionGenerator();
	private final SingleVehicleInsertionFilter insertionFilter;

	public ParallelMultiVehicleInsertionProblem(PrecalculablePathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer) {
		this.pathDataProvider = pathDataProvider;
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer);
		forkJoinPool = new ForkJoinPool(drtCfg.getNumberOfThreads());

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = 1.5 * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();// 1.5 is used to prevent filtering out feasible insertions
		insertionFilter = new SingleVehicleInsertionFilter(//
				new DetourTimesProvider(
						(from, to) -> DistanceUtils.calculateDistance(from, to) / optimisticBeelineSpeed,
						drtCfg.getStopDuration()), //
				new InsertionCostCalculator(drtCfg.getStopDuration(), timer));
	}

	@Override
	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		Map<Id<Vehicle>, List<Insertion>> filteredInsertionsPerVehicle = new HashMap<>();

		Map<Id<Link>, Link> linksToPickupMap = new HashMap<>();
		Map<Id<Link>, Link> linksFromPickupMap = new HashMap<>();
		Map<Id<Link>, Link> linksToDropoffMap = new HashMap<>();
		Map<Id<Link>, Link> linksFromDropoffMap = new HashMap<>();

		for (Entry vEntry : vEntries) {
			List<Insertion> insertions = insertionGenerator.generateInsertions(drtRequest, vEntry);

			List<InsertionWithDetourTimes> insertionsWithDetourTimes = insertionFilter
					.findFeasibleInsertions(drtRequest, vEntry, insertions);
			List<Insertion> filteredInsertions = new ArrayList<>(insertionsWithDetourTimes.size());
			filteredInsertionsPerVehicle.put(vEntry.vehicle.getId(), filteredInsertions);

			for (InsertionWithDetourTimes insert : insertionsWithDetourTimes) {
				int i = insert.getPickupIdx();
				int j = insert.getDropoffIdx();
				filteredInsertions.add(new Insertion(i, j));

				// i -> pickup
				Link toPickupLink = (i == 0) ? vEntry.start.link : vEntry.stops.get(i - 1).task.getLink();
				linksToPickupMap.put(toPickupLink.getId(), toPickupLink);

				// XXX optimise: if pickup/dropoff is inserted at existing stop,
				// no need to calc a path from pickup/dropoff to the next stop (the path is already in Schedule)

				if (i == j) {
					// pickup -> dropoff
					Link fromPickupLink = drtRequest.getToLink();
					linksFromPickupMap.put(fromPickupLink.getId(), fromPickupLink);
				} else {
					// pickup -> i + 1
					Link fromPickupLink = vEntry.stops.get(i).task.getLink();
					linksFromPickupMap.put(fromPickupLink.getId(), fromPickupLink);

					// j -> dropoff
					Link toDropoffLink = vEntry.stops.get(j - 1).task.getLink();
					linksToDropoffMap.put(toDropoffLink.getId(), toDropoffLink);
				}

				// dropoff -> j+1 // j+1 may not exist (dropoff appended after last stop)
				if (j < vEntry.stops.size()) {
					Link fromDropoffLink = vEntry.stops.get(j).task.getLink();
					linksFromDropoffMap.put(fromDropoffLink.getId(), fromDropoffLink);
				}
			}
		}

		pathDataProvider.precalculatePathData(drtRequest, linksToPickupMap, linksFromPickupMap, linksToDropoffMap,
				linksFromDropoffMap);

		return forkJoinPool.submit(() -> vEntries.parallelStream()//
				.map(v -> new SingleVehicleInsertionProblem(pathDataProvider, insertionCostCalculator)
						.findBestInsertion(drtRequest, v, filteredInsertionsPerVehicle.get(v.vehicle.getId())))//
				.filter(Optional::isPresent)//
				.map(Optional::get)//
				.min(Comparator.comparing(i -> i.cost)))//
				.join();
	}

	public void shutdown() {
		forkJoinPool.shutdown();
	}
}
