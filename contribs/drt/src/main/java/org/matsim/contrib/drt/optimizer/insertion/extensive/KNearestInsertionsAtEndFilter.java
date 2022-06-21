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

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.common.collections.PartialSort;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder.InsertionWithCost;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;

/**
 * "Insertion at end" means appending both pickup and dropoff at the end of the schedule, which means the ride
 * is not shared (like a normal taxi). In this case, the best insertion-at-end is the one that is closest in time,
 * so we just select the nearest (in straight-line) for the MultiNodeDijkstra (OneToManyPathSearch)
 *
 * @author michalm
 */
class KNearestInsertionsAtEndFilter {
	static List<Insertion> filterInsertionsAtEnd(int k, List<InsertionWithDetourData> insertions) {
		var nearestInsertionsAtEnd = new PartialSort<>(k, BestInsertionFinder.INSERTION_WITH_COST_COMPARATOR);
		var filteredInsertions = new ArrayList<Insertion>(insertions.size());

		for (var i : insertions) {
			var insertion = i.insertion;
			VehicleEntry vEntry = insertion.vehicleEntry;
			var pickup = insertion.pickup;
			if (!vEntry.isAfterLastStop(pickup.index)) {
				filteredInsertions.add(insertion);
			} else if (k > 0) {
				nearestInsertionsAtEnd.add(new InsertionWithCost(i, i.detourTimeInfo.pickupDetourInfo.departureTime));
			}
		}

		nearestInsertionsAtEnd.kSmallestElements()
				.forEach(i -> filteredInsertions.add(i.insertionWithDetourData.insertion));

		return filteredInsertions;
	}
}
