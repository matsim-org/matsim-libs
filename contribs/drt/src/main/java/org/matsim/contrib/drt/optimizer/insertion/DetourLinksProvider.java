/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.util.PartialSort;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
class DetourLinksProvider {

	// used to prevent filtering out feasible insertions
	private static final double OPTIMISTIC_BEELINE_SPEED_COEFF = 1.5;

	// "insertion at end" means appending both pickup and dropoff at the end of the schedule, which means the ride
	// is not shared (like a normal taxi). In this case, the best insertion-at-end is the one that is closest in time,
	// so we just select the nearest (in straight-line) for the MultiNodeDijkstra (OneToManyPathSearch)
	private static final int NEAREST_INSERTIONS_AT_END_LIMIT = 40;

	private final DrtRequest drtRequest;

	private final InsertionGenerator insertionGenerator = new InsertionGenerator();
	private final FeasibleInsertionFilter insertionFilter;

	private final DetourTimesProvider detourTimesProvider;

	// synchronised addition via addInsertionAtEndCandidate(InsertionAtEnd insertionAtEnd, double timeDistance)
	private final PartialSort<Insertion> nearestInsertionsAtEnd = new PartialSort<>(NEAREST_INSERTIONS_AT_END_LIMIT);

	public DetourLinksProvider(DrtConfigGroup drtCfg, MobsimTimer timer, DrtRequest drtRequest,
			InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.drtRequest = drtRequest;

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = OPTIMISTIC_BEELINE_SPEED_COEFF * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();

		detourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createBeelineTimeEstimator(optimisticBeelineSpeed));
		insertionFilter = FeasibleInsertionFilter.createWithDetourTimes(
				new InsertionCostCalculator(drtCfg, timer, penaltyCalculator));
	}

	List<Insertion> filterInsertions(ForkJoinPool forkJoinPool, Collection<Entry> vEntries) {
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> vEntries.parallelStream()// parallel outer stream
				.map(this::filterInsertions)//
				.flatMap(Collection::stream)// sequential inner stream
				.collect(Collectors.toList())).join();
		filteredInsertions.addAll(nearestInsertionsAtEnd.kSmallestElements());
		return filteredInsertions;
	}

	/**
	 * Designed to be called in parallel for each vEntry in VehicleData.entries
	 *
	 * @param vEntry
	 */
	private List<Insertion> filterInsertions(Entry vEntry) {
		List<Insertion> insertions = insertionGenerator.generateInsertions(drtRequest, vEntry);

		//optimistic pre-filtering (admissible cost function using an optimistic beeline speed coefficient)
		DetourDataProvider.DetourData<Double> data = detourTimesProvider.getDetourData(drtRequest);
		List<InsertionWithDetourData<Double>> insertionsWithDetourTimes = insertions.stream()
				.map(data::createInsertionWithDetourData)
				.filter(insertion -> insertionFilter.filter(drtRequest, insertion))
				.collect(Collectors.toList());
		if (insertionsWithDetourTimes.isEmpty()) {
			return List.of();
		}

		List<Insertion> filteredInsertions = new ArrayList<>(insertionsWithDetourTimes.size());
		for (InsertionWithDetourData<Double> insert : insertionsWithDetourTimes) {
			int i = insert.getPickupIdx();
			int j = insert.getDropoffIdx();

			if (i == vEntry.stops.size()) {// implies i == j
				double departureTime = vEntry.getWaypoint(i).getDepartureTime();
				// x OPTIMISTIC_BEELINE_SPEED_COEFF to remove bias towards near but still busy vehicles
				// (timeToPickup is underestimated by this factor)
				double timeDistance = departureTime + OPTIMISTIC_BEELINE_SPEED_COEFF * insert.getDetourToPickup();
				addInsertionAtEndCandidate(new Insertion(drtRequest, vEntry, i, j), timeDistance);
			} else {
				filteredInsertions.add(new Insertion(drtRequest, vEntry, i, j));
			}
		}

		return filteredInsertions;
	}

	private synchronized void addInsertionAtEndCandidate(Insertion insertionAtEnd, double timeDistance) {
		nearestInsertionsAtEnd.add(insertionAtEnd, timeDistance);
	}
}
