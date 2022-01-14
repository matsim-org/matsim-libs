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
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;

import com.google.common.base.MoreObjects;

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

	private static InsertionPoint createPickupInsertion(DrtRequest request, VehicleEntry vehicleEntry, int pickupIdx,
			boolean followedByDropoff) {
		var pickupWaypoint = new Waypoint.Pickup(request);
		var pickupPreviousWaypoint = vehicleEntry.getWaypoint(pickupIdx);
		var pickupNextLink = followedByDropoff ?
				new Waypoint.Dropoff(request) :
				vehicleEntry.getWaypoint(pickupIdx + 1);
		return new InsertionPoint(pickupIdx, pickupPreviousWaypoint, pickupWaypoint, pickupNextLink);
	}

	private static InsertionPoint createDropoffInsertion(DrtRequest request, VehicleEntry vehicleEntry,
			InsertionPoint pickup, int dropoffIdx) {
		var dropoffNextLink = vehicleEntry.getWaypoint(dropoffIdx + 1);

		if (pickup.index == dropoffIdx) {
			var dropoffPreviousLink = pickup.newWaypoint;
			var dropoffWaypoint = (Waypoint.Dropoff)pickup.nextWaypoint;
			return new InsertionPoint(dropoffIdx, dropoffPreviousLink, dropoffWaypoint, dropoffNextLink);
		}

		var dropoffPreviousLink = vehicleEntry.getWaypoint(dropoffIdx);
		var dropoffWaypoint = new Waypoint.Dropoff(request);
		return new InsertionPoint(dropoffIdx, dropoffPreviousLink, dropoffWaypoint, dropoffNextLink);
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

		Insertion(DrtRequest request, VehicleEntry vehicleEntry, int pickupIdx, int dropoffIdx) {
			this.vehicleEntry = vehicleEntry;
			pickup = createPickupInsertion(request, vehicleEntry, pickupIdx, pickupIdx == dropoffIdx);
			dropoff = createDropoffInsertion(request, vehicleEntry, pickup, dropoffIdx);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("vehicleId", vehicleEntry.vehicle.getId())
					.add("pickup", pickup)
					.add("dropoff", dropoff)
					.toString();
		}
	}

	private final DetourTimeEstimator detourTimeEstimator;
	private final InsertionDetourTimeCalculator<Double> detourTimeCalculator;

	public InsertionGenerator(double stopDuration, DetourTimeEstimator detourTimeEstimator) {
		this.detourTimeEstimator = detourTimeEstimator;
		detourTimeCalculator = new InsertionDetourTimeCalculator<>(stopDuration, Double::doubleValue,
				detourTimeEstimator);
	}

	public List<InsertionWithDetourData<Double>> generateInsertions(DrtRequest drtRequest, VehicleEntry vEntry) {
		int stopCount = vEntry.stops.size();
		List<InsertionWithDetourData<Double>> insertions = new ArrayList<>();
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

	private void generateDropoffInsertions(DrtRequest request, VehicleEntry vEntry, int i,
			List<InsertionWithDetourData<Double>> insertions) {
		var pickupInsertion = createPickupInsertion(request, vEntry, i, true);
		double toPickupTT = detourTimeEstimator.estimateTime(pickupInsertion.previousWaypoint.getLink(),
				request.getFromLink());
		double fromPickupTT = detourTimeEstimator.estimateTime(request.getFromLink(),
				pickupInsertion.nextWaypoint.getLink());
		var pickupDetourInfo = detourTimeCalculator.calcPickupDetourInfo(vEntry, pickupInsertion, toPickupTT,
				fromPickupTT, true);

		boolean recalculated = false;

		int stopCount = vEntry.stops.size();
		for (int j = i; j < stopCount; j++) {// insertions up to before last stop
			// i -> pickup -> i+1 && j -> dropoff -> j+1
			if (j == i + 1) {
				//calculate it once for all j > i
				pickupInsertion = createPickupInsertion(request, vEntry, i, false);
				fromPickupTT = detourTimeEstimator.estimateTime(request.getFromLink(),
						pickupInsertion.nextWaypoint.getLink());
				pickupDetourInfo = detourTimeCalculator.calcPickupDetourInfo(vEntry, pickupInsertion, toPickupTT,
						fromPickupTT, false);
				recalculated = true;
			}

			if (j > i) {// no need to check the capacity constraints if i == j (already validated for `i`)
				Waypoint.Stop currentStop = currentStop(vEntry, j);
				if (currentStop.outgoingOccupancy == vEntry.vehicle.getCapacity()) {
					if (request.getToLink() == currentStop.task.getLink()) {
						//special case -- we can insert dropoff exactly at node j
						insertions.add(createInsertionWithDetourData(request, vEntry, pickupInsertion, toPickupTT,
								fromPickupTT, pickupDetourInfo, j));
					}

					return;// stop iterating -- cannot insert dropoff after node j
				}
			}

			if (request.getToLink() != nextStop(vEntry, j).task.getLink()) {// next stop at different link
				insertions.add(createInsertionWithDetourData(request, vEntry, pickupInsertion, toPickupTT, fromPickupTT,
						pickupDetourInfo, j));
			}
			// else: do not evaluate insertion _before_stop j, evaluate only insertion _after_ stop j
		}

		// insertion after last stop
		if (!recalculated && i < stopCount) {
			pickupInsertion = createPickupInsertion(request, vEntry, i, false);
			fromPickupTT = detourTimeEstimator.estimateTime(request.getFromLink(),
					pickupInsertion.nextWaypoint.getLink());
			pickupDetourInfo = detourTimeCalculator.calcPickupDetourInfo(vEntry, pickupInsertion, toPickupTT,
					fromPickupTT, false);
		}

		insertions.add(createInsertionWithDetourData(request, vEntry, pickupInsertion, toPickupTT, fromPickupTT,
				pickupDetourInfo, stopCount));
	}

	private Waypoint.Stop currentStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx - 1);
	}

	private Waypoint.Stop nextStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx);
	}

	private InsertionWithDetourData<Double> createInsertionWithDetourData(DrtRequest request, VehicleEntry vehicleEntry,
			InsertionPoint pickupInsertion, double toPickupTT, double fromPickupTT, PickupDetourInfo pickupDetourInfo,
			int dropoffIdx) {
		var dropoffInsertion = createDropoffInsertion(request, vehicleEntry, pickupInsertion, dropoffIdx);
		var insertion = new Insertion(vehicleEntry, pickupInsertion, dropoffInsertion);

		double toDropoffTT = pickupInsertion.index == dropoffIdx ?
				fromPickupTT :
				detourTimeEstimator.estimateTime(dropoffInsertion.previousWaypoint.getLink(), request.getToLink());
		double fromDropoffTT = dropoffIdx == vehicleEntry.stops.size() ?
				0 :
				detourTimeEstimator.estimateTime(request.getToLink(), dropoffInsertion.nextWaypoint.getLink());
		var insertionDetourData = new InsertionDetourData<>(toPickupTT, fromPickupTT, toDropoffTT, fromDropoffTT);

		var dropoffDetourInfo = detourTimeCalculator.calcDropoffDetourInfo(insertion, toDropoffTT, fromDropoffTT,
				pickupDetourInfo);
		return new InsertionWithDetourData<>(insertion, insertionDetourData,
				new DetourTimeInfo(pickupDetourInfo, dropoffDetourInfo));
	}
}
