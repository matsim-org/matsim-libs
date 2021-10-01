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

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Generates all possible pickup and dropoff insertion point pairs that do not violate the vehicle capacity. In order to
 * generate insertion points for a given vehicle, a {@code VehicleEntry} must be prepared. It contains the
 * information about the vehicle current state (location, time and occupancy; additionally, the vehicle can be queried
 * about the current task etc. ) and the sequence of planned stops (of length {@code N}.
 * <p>
 * Pickup insertion points are indexed in the following way:
 * <ul>
 * <li>{@code pickupIdx == 0} - pickup inserted at/after the current position (e.g. ongoing stop, stay or drive, the
 * latter resulting in immediate diversion)</li>
 * <li>{@code 0 < pickupIdx <= stops.size()} - pickup inserted at/after stop {@code pickupIdx - 1}</li>
 * </ul>
 * <p>
 * Dropoff insertion points are indexed in the following way:
 * <ul>
 * <li>{@code dropoffIdx == pickupIdx} - dropoff inserted directly after pickup</li>
 * <li>{@code pickupIdx < dropoffIdx <= stops.size()} - dropoff inserted at/after stop {@code dropoffIdx - 1}</li>
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
	public static class InsertionPoint {
		public final int index;
		public final Waypoint previousWaypoint;
		public final Waypoint newWaypoint;
		public final Waypoint nextWaypoint;

		public InsertionPoint(int index, Waypoint previousWaypoint, Waypoint newWaypoint, Waypoint nextWaypoint) {
			this.index = index;
			this.previousWaypoint = previousWaypoint;
			this.newWaypoint = newWaypoint;
			this.nextWaypoint = nextWaypoint;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("index", index)
					.add("previousWaypoint", previousWaypoint)
					.add("newWaypoint", newWaypoint)
					.add("nextWaypoint", nextWaypoint)
					.toString();
		}
	}

	public static class Insertion {
		public final VehicleEntry vehicleEntry;
		public final InsertionPoint pickup;
		public final InsertionPoint dropoff;

		public Insertion(VehicleEntry vehicleEntry, InsertionPoint pickup, InsertionPoint dropoff) {
			this.vehicleEntry = vehicleEntry;
			this.pickup = pickup;
			this.dropoff = dropoff;
		}

		public Insertion(DrtRequest request, VehicleEntry vehicleEntry, int pickupIdx, int dropoffIdx) {
			this.vehicleEntry = vehicleEntry;

			Waypoint.Pickup pickupWaypoint = new Waypoint.Pickup(request);
			Waypoint.Dropoff dropoffWaypoint = new Waypoint.Dropoff(request);

			Waypoint pickupPreviousWaypoint = vehicleEntry.getWaypoint(pickupIdx);
			Waypoint pickupNextLink = pickupIdx == dropoffIdx ?
					dropoffWaypoint :
					vehicleEntry.getWaypoint(pickupIdx + 1);
			pickup = new InsertionPoint(pickupIdx, pickupPreviousWaypoint, pickupWaypoint, pickupNextLink);

			Waypoint dropoffPreviousLink = pickupIdx == dropoffIdx ?
					pickupWaypoint :
					vehicleEntry.getWaypoint(dropoffIdx);
			Waypoint dropoffNextLink = vehicleEntry.getWaypoint(dropoffIdx + 1);
			dropoff = new InsertionPoint(dropoffIdx, dropoffPreviousLink, dropoffWaypoint, dropoffNextLink);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("vehicleId", vehicleEntry.vehicle.getId())
					.add("pickup", pickup)
					.add("dropoff", dropoff)
					.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Insertion insertion = (Insertion)o;
			return Objects.equal(vehicleEntry, insertion.vehicleEntry)
					&& Objects.equal(pickup, insertion.pickup)
					&& Objects.equal(dropoff, insertion.dropoff);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(vehicleEntry, pickup, dropoff);
		}
	}

	public List<Insertion> generateInsertions(DrtRequest drtRequest, VehicleEntry vEntry) {
		int stopCount = vEntry.stops.size();
		List<Insertion> insertions = new ArrayList<>();
		int occupancy = vEntry.start.occupancy;
		for (int i = 0; i < stopCount; i++) {// insertions up to before last stop
			Waypoint.Stop nextStop = nextStop(vEntry, i);

			if (occupancy < vEntry.vehicle.getCapacity()) {// only not fully loaded arcs
				if (drtRequest.getFromLink() != nextStop.task.getLink()) {// next stop at different link
					generateDropoffInsertions(drtRequest, vEntry, i, insertions);
				}
				// else: do not evaluate insertion _before_stop i, evaluate only insertion _after_ stop i
			}

			occupancy = nextStop.outgoingOccupancy;
		}

		generateDropoffInsertions(drtRequest, vEntry, stopCount, insertions);// at/after last stop
		return insertions;
	}

	//TODO replace argument: int i -> InsertionPoint pickup//?
	private void generateDropoffInsertions(DrtRequest drtRequest, VehicleEntry vEntry, int i,
			List<Insertion> insertions) {
		int stopCount = vEntry.stops.size();
		for (int j = i; j < stopCount; j++) {// insertions up to before last stop
			// i -> pickup -> i+1 && j -> dropoff -> j+1

			if (j > i) {// no need to check the capacity constraints if i == j (already validated for `i`)
				Waypoint.Stop currentStop = currentStop(vEntry, j);
				if (currentStop.outgoingOccupancy == vEntry.vehicle.getCapacity()) {
					if (drtRequest.getToLink() == currentStop.task.getLink()) {
						//special case -- we can insert dropoff exactly at node j
						insertions.add(new Insertion(drtRequest, vEntry, i, j));
					}

					return;// stop iterating -- cannot insert dropoff after node j
				}
			}

			if (drtRequest.getToLink() != nextStop(vEntry, j).task.getLink()) {// next stop at different link
				insertions.add(new Insertion(drtRequest, vEntry, i, j));
			}
			// else: do not evaluate insertion _before_stop j, evaluate only insertion _after_ stop j
		}

		insertions.add(new Insertion(drtRequest, vEntry, i, stopCount));// insertion after last stop
	}

	private Waypoint.Stop currentStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx - 1);
	}

	private Waypoint.Stop nextStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx);
	}
}
