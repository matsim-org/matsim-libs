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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Vehicle;
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

	private final InsertionGenerator insertionGenerator = new InsertionGenerator();
	private final SingleVehicleInsertionFilter insertionFilter;

	private final Map<Id<Vehicle>, List<Insertion>> filteredInsertionsPerVehicle;

	private final Map<Id<Link>, Link> linksToPickup;
	private final Map<Id<Link>, Link> linksFromPickup;
	private final Map<Id<Link>, Link> linksToDropoff;
	private final Map<Id<Link>, Link> linksFromDropoff;

	// synchronised addition via addInsertionAtEndCandidate(InsertionAtEnd insertionAtEnd, double timeDistance)
	private final PartialSort<InsertionAtEnd> nearestInsertionsAtEnd = new PartialSort<>(40);

	private static class InsertionAtEnd {
		private final Entry vEntry;
		private final InsertionWithDetourTimes insertion;

		private InsertionAtEnd(Entry vEntry, InsertionWithDetourTimes insertion) {
			this.vEntry = vEntry;
			this.insertion = insertion;
		}
	}

	public DetourLinksProvider(DrtConfigGroup drtCfg, MobsimTimer timer, int vEntriesCount) {
		filteredInsertionsPerVehicle = new ConcurrentHashMap<>(vEntriesCount);
		linksToPickup = new ConcurrentHashMap<>(vEntriesCount / 2);
		linksFromPickup = new ConcurrentHashMap<>();
		linksToDropoff = new ConcurrentHashMap<>();
		linksFromDropoff = new ConcurrentHashMap<>();

		// TODO use more sophisticated DetourTimeEstimator
		double optimisticBeelineSpeed = OPTIMISTIC_BEELINE_SPEED_COEFF * drtCfg.getEstimatedDrtSpeed()
				/ drtCfg.getEstimatedBeelineDistanceFactor();
		insertionFilter = new SingleVehicleInsertionFilter(//
				new DetourTimesProvider(
						(from, to) -> DistanceUtils.calculateDistance(from, to) / optimisticBeelineSpeed,
						drtCfg.getStopDuration()), //
				new InsertionCostCalculator(drtCfg, timer));
	}

	/**
	 * Designed to be called in parallel for each vEntry in VehicleData.entries
	 * 
	 * @param drtRequest
	 * @param vEntry
	 */
	void addDetourLinks(DrtRequest drtRequest, Entry vEntry) {
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
				addLinks(i, j, vEntry, drtRequest);
			}
		}

		if (!filteredInsertions.isEmpty()) {
			filteredInsertionsPerVehicle.put(vEntry.vehicle.getId(), filteredInsertions);
		}
	}

	void processNearestInsertionsAtEnd(DrtRequest drtRequest) {
		List<InsertionAtEnd> insertionsAtEnd = nearestInsertionsAtEnd.kSmallestElements();
		for (InsertionAtEnd iAtEnd : insertionsAtEnd) {
			int i = iAtEnd.insertion.getPickupIdx();
			int j = iAtEnd.insertion.getDropoffIdx();
			filteredInsertionsPerVehicle.computeIfAbsent(iAtEnd.vEntry.vehicle.getId(), k -> new ArrayList<>())
					.add(new Insertion(i, j));
			addLinks(i, j, iAtEnd.vEntry, drtRequest);
		}
	}

	private synchronized void addInsertionAtEndCandidate(InsertionAtEnd insertionAtEnd, double timeDistance) {
		nearestInsertionsAtEnd.add(insertionAtEnd, timeDistance);
	}

	private void addLinks(int i, int j, Entry vEntry, DrtRequest drtRequest) {
		// i -> pickup
		Link toPickupLink = (i == 0) ? vEntry.start.link : vEntry.stops.get(i - 1).task.getLink();
		linksToPickup.putIfAbsent(toPickupLink.getId(), toPickupLink);

		// XXX optimise: if pickup/dropoff is inserted at existing stop,
		// no need to calc a path from pickup/dropoff to the next stop (the path is already in Schedule)

		if (i == j) {
			// pickup -> dropoff
			Link fromPickupLink = drtRequest.getToLink();
			linksFromPickup.putIfAbsent(fromPickupLink.getId(), fromPickupLink);
		} else {
			// pickup -> i + 1
			Link fromPickupLink = vEntry.stops.get(i).task.getLink();
			linksFromPickup.putIfAbsent(fromPickupLink.getId(), fromPickupLink);

			// j -> dropoff
			Link toDropoffLink = vEntry.stops.get(j - 1).task.getLink();
			linksToDropoff.putIfAbsent(toDropoffLink.getId(), toDropoffLink);
		}

		// dropoff -> j+1 // j+1 may not exist (dropoff appended after last stop)
		if (j < vEntry.stops.size()) {
			Link fromDropoffLink = vEntry.stops.get(j).task.getLink();
			linksFromDropoff.putIfAbsent(fromDropoffLink.getId(), fromDropoffLink);
		}

	}

	Map<Id<Vehicle>, List<Insertion>> getFilteredInsertionsPerVehicle() {
		return filteredInsertionsPerVehicle;
	}

	DetourLinksSet getDetourLinksSet() {
		return new DetourLinksSet(linksToPickup, linksFromPickup, linksToDropoff, linksFromDropoff);
	}
}
