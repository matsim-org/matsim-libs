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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
class DetourLinksProvider {
	static class DetourLinksSet {
		final Map<Id<Link>, Link> pickupDetourStartLinks;
		final Map<Id<Link>, Link> pickupDetourEndLinks;
		final Map<Id<Link>, Link> dropoffDetourStartLinks;
		final Map<Id<Link>, Link> dropoffDetourEndLinks;

		public DetourLinksSet(Map<Id<Link>, Link> linksToPickup, Map<Id<Link>, Link> linksFromPickup,
				Map<Id<Link>, Link> linksToDropoff, Map<Id<Link>, Link> linksFromDropoff) {
			this.pickupDetourStartLinks = linksToPickup;
			this.pickupDetourEndLinks = linksFromPickup;
			this.dropoffDetourStartLinks = linksToDropoff;
			this.dropoffDetourEndLinks = linksFromDropoff;
		}
	}

	// used to prevent filtering out feasible insertions
	private static final double OPTIMISTIC_BEELINE_SPEED_COEFF = 1.5;

	// "insertion at end" means appending both pickup and dropoff at the end of the schedule, which means the ride
	// is not shared (like a normal taxi). In this case, the best insertion-at-end is the one that is closest in time,
	// so we just select the nearest (in straight-line) for the MultiNodeDijkstra (OneToManyPathSearch)
	private static final int NEAREST_INSERTIONS_AT_END_LIMIT = 40;

	private final DrtRequest drtRequest;

	private final InsertionGenerator insertionGenerator = new InsertionGenerator();
	private final SingleVehicleInsertionFilter insertionFilter;

	private DetourLinksSet detourLinksSet;
	private final Map<Entry, List<Insertion>> filteredInsertionsPerVehicle;

	private final Map<Id<Link>, Link> linksToPickup;
	private final Map<Id<Link>, Link> linksFromPickup;
	private final Map<Id<Link>, Link> linksToDropoff;
	private final Map<Id<Link>, Link> linksFromDropoff;

	// synchronised addition via addInsertionAtEndCandidate(InsertionAtEnd insertionAtEnd, double timeDistance)
	private final PartialSort<InsertionAtEnd> nearestInsertionsAtEnd = new PartialSort<>(
			NEAREST_INSERTIONS_AT_END_LIMIT);

	private static class InsertionAtEnd {
		private final Entry vEntry;
		private final InsertionWithDetourTimes insertion;

		private InsertionAtEnd(Entry vEntry, InsertionWithDetourTimes insertion) {
			this.vEntry = vEntry;
			this.insertion = insertion;
		}
	}

	public DetourLinksProvider(DrtConfigGroup drtCfg, MobsimTimer timer, DrtRequest drtRequest,
			InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.drtRequest = drtRequest;

		// initial capacities of concurrent maps according to insertion stats for AT Berlin 10pct
		// in general, for larger fleets they should be slightly higher than for smaller fleets,
		// nevertheless NEAREST_INSERTIONS_AT_END_LIMIT keeps them more independent of the fleet size
		filteredInsertionsPerVehicle = new ConcurrentHashMap<>(NEAREST_INSERTIONS_AT_END_LIMIT * 2);
		linksToPickup = new ConcurrentHashMap<>(NEAREST_INSERTIONS_AT_END_LIMIT);
		linksFromPickup = new ConcurrentHashMap<>(NEAREST_INSERTIONS_AT_END_LIMIT / 2);
		linksToDropoff = new ConcurrentHashMap<>(NEAREST_INSERTIONS_AT_END_LIMIT / 2);
		linksFromDropoff = new ConcurrentHashMap<>();

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = OPTIMISTIC_BEELINE_SPEED_COEFF * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();
		insertionFilter = new SingleVehicleInsertionFilter(new DetourTimesProvider(
				(from, to) -> DistanceUtils.calculateDistance(from, to) / optimisticBeelineSpeed,
				drtCfg.getStopDuration()), new InsertionCostCalculator(drtCfg, timer, penaltyCalculator));
	}

	void findInsertionsAndLinks(ForkJoinPool forkJoinPool, Collection<Entry> vEntries) {
		forkJoinPool.submit(() -> vEntries.parallelStream().forEach(this::addDetourLinks)).join();
		processNearestInsertionsAtEnd();
		detourLinksSet = new DetourLinksSet(linksToPickup, linksFromPickup, linksToDropoff, linksFromDropoff);
	}

	/**
	 * Designed to be called in parallel for each vEntry in VehicleData.entries
	 *
	 * @param vEntry
	 */
	private void addDetourLinks(Entry vEntry) {
		List<Insertion> insertions = insertionGenerator.generateInsertions(drtRequest, vEntry);
		List<InsertionWithDetourTimes> insertionsWithDetourTimes = insertionFilter.findFeasibleInsertions(drtRequest,
				vEntry, insertions);
		if (insertionsWithDetourTimes.isEmpty()) {
			return;
		}

		List<Insertion> filteredInsertions = new ArrayList<>(insertionsWithDetourTimes.size());
		for (InsertionWithDetourTimes insert : insertionsWithDetourTimes) {
			int i = insert.getPickupIdx();
			int j = insert.getDropoffIdx();

			if (i == j && i == vEntry.stops.size()) {
				double departureTime = (i == 0) ? vEntry.start.time : vEntry.stops.get(i - 1).task.getEndTime();
				// x OPTIMISTIC_BEELINE_SPEED_COEFF to remove bias towards near but still busy vehicles
				// (timeToPickup is underestimated by this factor)
				double timeDistance = departureTime + OPTIMISTIC_BEELINE_SPEED_COEFF * insert.getTimeToPickup();
				addInsertionAtEndCandidate(new InsertionAtEnd(vEntry, insert), timeDistance);
			} else {
				filteredInsertions.add(new Insertion(i, j));
				addLinks(i, j, vEntry);
			}
		}

		if (!filteredInsertions.isEmpty()) {
			filteredInsertionsPerVehicle.put(vEntry, filteredInsertions);
		}
	}

	private void processNearestInsertionsAtEnd() {
		List<InsertionAtEnd> insertionsAtEnd = nearestInsertionsAtEnd.kSmallestElements();
		for (InsertionAtEnd iAtEnd : insertionsAtEnd) {
			int i = iAtEnd.insertion.getPickupIdx();
			int j = iAtEnd.insertion.getDropoffIdx();
			filteredInsertionsPerVehicle.computeIfAbsent(iAtEnd.vEntry, k -> new ArrayList<>())
					.add(new Insertion(i, j));
			addLinks(i, j, iAtEnd.vEntry);
		}
	}

	private synchronized void addInsertionAtEndCandidate(InsertionAtEnd insertionAtEnd, double timeDistance) {
		nearestInsertionsAtEnd.add(insertionAtEnd, timeDistance);
	}

	private void addLinks(int i, int j, Entry vEntry) {
		// i -> pickup
		putLinkToMap(linksToPickup, (i == 0) ? vEntry.start.link : vEntry.stops.get(i - 1).task.getLink());

		// XXX optimise: if pickup/dropoff is inserted at existing stop,
		// no need to calc a path from pickup/dropoff to the next stop (the path is already in Schedule)

		if (i == j) {
			// pickup -> dropoff
			putLinkToMap(linksFromPickup, drtRequest.getToLink());
		} else {
			// pickup -> i + 1
			putLinkToMap(linksFromPickup, vEntry.stops.get(i).task.getLink());

			// j -> dropoff
			putLinkToMap(linksToDropoff, vEntry.stops.get(j - 1).task.getLink());
		}

		// dropoff -> j+1 // j+1 may not exist (dropoff appended after last stop)
		if (j < vEntry.stops.size()) {
			putLinkToMap(linksFromDropoff, vEntry.stops.get(j).task.getLink());
		}
	}

	private void putLinkToMap(Map<Id<Link>, Link> map, Link link) {
		map.putIfAbsent(link.getId(), link);
	}

	Map<Entry, List<Insertion>> getFilteredInsertions() {
		return filteredInsertionsPerVehicle;
	}

	DetourLinksSet getDetourLinksSet() {
		return detourLinksSet;
	}
}
