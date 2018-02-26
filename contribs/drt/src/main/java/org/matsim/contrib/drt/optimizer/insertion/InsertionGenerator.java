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

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;

/**
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
	}

	public List<Insertion> generateInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		int stopCount = vEntry.stops.size();
		List<Insertion> insertions = new ArrayList<>();
		int occupancy = vEntry.startOccupancy;
		for (int i = 0; i < stopCount; i++) {// insertions up to before last stop
			// pickup is inserted after node i, where
			// node 0 is 'start' (current position/immediate diversion point)
			// node i > 0 is (i-1)th 'stop task'
			// replacing i -> i+1 with i -> pickup -> i+1 means all following stop tasks are affected
			// (==> calc delay for tasks i to n ==> calc cost)

			if (occupancy < vEntry.vehicle.getCapacity() // only not fully loaded arcs
					&& drtRequest.getFromLink() != vEntry.stops.get(i).task.getLink()) {// next stop is at the same link
				// optimize for cases where the pickup is at the same link as stop i (i.e. node i+1)
				// in this case inserting the pickup either before and after the stop is equivalent
				// ==> only evaluate insertion _after_ stop i (node i+1)
				generateDropoffInsertions(drtRequest, vEntry, i, insertions);
			}
			occupancy = vEntry.stops.get(i).outgoingOccupancy;
		}

		generateDropoffInsertions(drtRequest, vEntry, stopCount, insertions);// last stop
		return insertions;
	}

	private void generateDropoffInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry, int i,
			List<Insertion> insertions) {
		int stopCount = vEntry.stops.size();
		for (int j = i; j < stopCount; j++) {// insertions up to before last stop
			// dropoff is inserted after node j, where
			// node j=i is 'pickup'
			// node j>i is (j-1)th 'stop task'
			// replacing j -> j+1 with j -> dropoff -> j+1 ==> all following stop tasks are affected
			// (==> calc delay for tasks j to n ==> calc cost)

			// no checking the capacity constraints if i == j
			if (j > i && // i -> pickup -> i+1 && j -> dropoff -> j+1
					vEntry.stops.get(j - 1).outgoingOccupancy == vEntry.vehicle.getCapacity()) {
				return;// stop iterating -- cannot insert dropoff after node j
			}

			if (drtRequest.getToLink() != vEntry.stops.get(j).task.getLink()) {// next stop is at the same link
				// optimize for cases where the dropoff is at the same link as stop j-1 (i.e. node j)
				// in this case inserting the dropoff either before and after the stop is equivalent
				// ==> only evaluate insertion _after_ stop j (node j+1)
				insertions.add(new Insertion(i, j));
			}
		}
		
		insertions.add(new Insertion(i, stopCount));// insertion after last stop
	}
}
