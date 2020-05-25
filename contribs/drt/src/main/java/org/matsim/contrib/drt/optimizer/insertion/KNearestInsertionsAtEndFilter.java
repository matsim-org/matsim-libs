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

import static org.matsim.contrib.drt.optimizer.insertion.ParallelMultiVehicleInsertionProblem.OPTIMISTIC_BEELINE_SPEED_COEFF;

import java.util.List;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.util.PartialSort;

/**
 * "Insertion at end" means appending both pickup and dropoff at the end of the schedule, which means the ride
 * is not shared (like a normal taxi). In this case, the best insertion-at-end is the one that is closest in time,
 * so we just select the nearest (in straight-line) for the MultiNodeDijkstra (OneToManyPathSearch)
 *
 * @author michalm
 */
class KNearestInsertionsAtEndFilter {
	// synchronised addition via addInsertionAtEndCandidate(Insertion insertionAtEnd, double timeDistance)
	private final PartialSort<Insertion> nearestInsertionsAtEnd;

	public KNearestInsertionsAtEndFilter(int k) {
		nearestInsertionsAtEnd = new PartialSort<>(k);
	}

	/**
	 * Designed to be used with parallel streams
	 */
	boolean filter(InsertionWithDetourData<Double> insertion) {
		Entry vEntry = insertion.getVehicleEntry();
		int i = insertion.getPickup().index;

		if (i < vEntry.stops.size()) {//not an insertion at the schedule end
			return true;
		}

		//i == j == stops.size()
		double departureTime = vEntry.getWaypoint(i).getDepartureTime();

		// x OPTIMISTIC_BEELINE_SPEED_COEFF to remove bias towards near but still busy vehicles
		// (timeToPickup is underestimated by this factor)
		double timeDistance = departureTime + OPTIMISTIC_BEELINE_SPEED_COEFF * insertion.getDetourToPickup();
		addInsertionAtEndCandidate(insertion.getInsertion(), timeDistance);
		return false;//skip now; the selected (i.e. K nearest) insertions will be added later
	}

	public List<Insertion> getNearestInsertionsAtEnd() {
		return nearestInsertionsAtEnd.kSmallestElements();
	}

	//synchronized -- allows filtering of parallel streams
	private synchronized void addInsertionAtEndCandidate(Insertion insertionAtEnd, double timeDistance) {
		nearestInsertionsAtEnd.add(insertionAtEnd, timeDistance);
	}
}
