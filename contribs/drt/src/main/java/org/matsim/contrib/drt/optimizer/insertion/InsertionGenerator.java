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
import java.util.List;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;

/**
 * Generates all possible pickup and dropoff insertion point pairs that do not violate the vehicle capacity. In order to
 * generate insertion points for a given vehicle, a {@code VehicleData.Entry} must be prepared. It contains the
 * information about the vehicle current state (location, time and occupancy; additionally, the vehicle can be queried
 * about the current task etc. ) and the sequence of planned stops (of lenght {@code N}.
 * <p>
 * Pickup insertion points are indexed in the following way:
 * <ul>
 * <li>{@code pickupIdx = 0} - pickup inserted at/after the current position (e.g. ongoing stop, stay or drive, the
 * latter resulting in immediate diversion)</li>
 * <li>{@code 0 < pickupIdx <= N} - pickup inserted at/after stop {@code pickupIdx - 1}</li>
 * </ul>
 * <p>
 * Dropoff insertion points are indexed in the following way:
 * <ul>
 * <li>{@code dropoffIdx = pickupIdx} - dropoff inserted directly after pickup</li>
 * <li>{@code pickupIdx < dropoffIdx <= N} - dropoff inserted at/after stop {@code dropoffIdx - 1}</li>
 * </ul>
 * <p>
 * A pickup/dropoff is inserted at (i.e. included into) a given stop if they are on the same link. Otherwise, it is
 * inserted after that stop. In the latter case, the sequence of stops is changed from: <br/>
 * {@code ... --> stop i --> stop i + 1 --> ...} <br/>
 * to: <br/>
 * {@code ... --> stop i --> new stop --> stop i + 1 --> ...}.
 * <p>
 * If a pickup/dropoff is inserted at stop {@code i} (pickup/dropoff is on the same link as the stop), insertion after
 * stop {@code i-1} will not be generated (as that would be equivalent to/duplicate of the former one).
 * 
 * @author michalm
 */
public class InsertionGenerator {
	public static class Insertion {
		public final int pickupIdx;
		public final int dropoffIdx;

		public Insertion(int pickupIdx, int dropoffIdx) {
			this.pickupIdx = pickupIdx;
			this.dropoffIdx = dropoffIdx;
		}

		@Override
		public String toString() {
			return "[pickupIdx=" + pickupIdx + "][dropoffIdx=" + dropoffIdx + "]";
		}
	}

	public List<Insertion> generateInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		int stopCount = vEntry.stops.size();
		List<Insertion> insertions = new ArrayList<>();
		int occupancy = vEntry.startOccupancy;
		for (int i = 0; i < stopCount; i++) {// insertions up to before last stop
			if (occupancy < vEntry.vehicle.getCapacity() // only not fully loaded arcs
					&& drtRequest.getFromLink() != vEntry.stops.get(i).task.getLink()) {// next stop at different link
				generateDropoffInsertions(drtRequest, vEntry, i, insertions);
			}
			// else: skip this stop and evaluate only insertion _after_ stop i

			occupancy = vEntry.stops.get(i).outgoingOccupancy;
		}

		generateDropoffInsertions(drtRequest, vEntry, stopCount, insertions);// last stop
		return insertions;
	}

	private void generateDropoffInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry, int i,
			List<Insertion> insertions) {
		int stopCount = vEntry.stops.size();
		for (int j = i; j < stopCount; j++) {// insertions up to before last stop
			// no need to check the capacity constraints if i == j
			if (j > i && // i -> pickup -> i+1 && j -> dropoff -> j+1
					vEntry.stops.get(j - 1).outgoingOccupancy == vEntry.vehicle.getCapacity()) {
				return;// stop iterating -- cannot insert dropoff after node j
			}

			if (drtRequest.getToLink() != vEntry.stops.get(j).task.getLink()) {// next stop at different link
				insertions.add(new Insertion(i, j));
			}
			// else: skip this stop and evaluate only insertion _after_ stop i
		}

		insertions.add(new Insertion(i, stopCount));// insertion after last stop
	}
}
