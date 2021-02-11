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

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder.InsertionWithCost;
import org.matsim.contrib.util.PartialSort;

/**
 * "Insertion at end" means appending both pickup and dropoff at the end of the schedule, which means the ride
 * is not shared (like a normal taxi). In this case, the best insertion-at-end is the one that is closest in time,
 * so we just select the nearest (in straight-line) for the MultiNodeDijkstra (OneToManyPathSearch)
 *
 * @author michalm
 */
class KNearestInsertionsAtEndFilter {
	static List<InsertionGenerator.Insertion> filterInsertionsAtEnd(int k, double admissibleBeelineSpeedFactor,
			List<InsertionWithDetourData<Double>> insertions) {
		var nearestInsertionsAtEnd = new PartialSort<InsertionWithCost<Double>>(k,
				BestInsertionFinder.createInsertionWithCostComparator());
		var filteredInsertions = new ArrayList<InsertionGenerator.Insertion>(insertions.size());

		for (var insertion : insertions) {
			VehicleEntry vEntry = insertion.getVehicleEntry();
			var pickup = insertion.getPickup();
			if (!vEntry.isAfterLastStop(pickup.index)) {
				filteredInsertions.add(insertion.getInsertion());
			} else if (k > 0) {
				double departureTime = pickup.previousWaypoint.getDepartureTime();

				// x ADMISSIBLE_BEELINE_SPEED_FACTOR to remove bias towards near but still busy vehicles
				// (timeToPickup is underestimated by this factor)
				double timeDistance = departureTime + admissibleBeelineSpeedFactor * insertion.getDetourToPickup();
				nearestInsertionsAtEnd.add(new InsertionWithCost<>(insertion, timeDistance));
			}
		}

		nearestInsertionsAtEnd.kSmallestElements()
				.forEach(i -> filteredInsertions.add(i.insertionWithDetourData.getInsertion()));

		return filteredInsertions;
	}
}
