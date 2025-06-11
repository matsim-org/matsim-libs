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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.schedule.CapacityChangeTask;
import org.matsim.contrib.dvrp.schedule.Task;

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
		public final DvrpLoad insertedLoad;

		public Insertion(VehicleEntry vehicleEntry, InsertionPoint pickup, InsertionPoint dropoff, DvrpLoad insertedLoad) {
			this.vehicleEntry = vehicleEntry;
			this.pickup = pickup;
			this.dropoff = dropoff;
			this.insertedLoad = insertedLoad;
		}

		public Insertion(DrtRequest request, VehicleEntry vehicleEntry, int pickupIdx, int dropoffIdx) {
			this.vehicleEntry = vehicleEntry;
			pickup = createPickupInsertion(request, vehicleEntry, pickupIdx, pickupIdx == dropoffIdx);
			dropoff = createDropoffInsertion(request, vehicleEntry, pickup, dropoffIdx);
			this.insertedLoad = request.getLoad();
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
	private final InsertionDetourTimeCalculator detourTimeCalculator;

	public InsertionGenerator(StopTimeCalculator stopTimeCalculator, DetourTimeEstimator detourTimeEstimator) {
		this.detourTimeEstimator = detourTimeEstimator;
		detourTimeCalculator = new InsertionDetourTimeCalculator(stopTimeCalculator, detourTimeEstimator);
	}

	public List<InsertionWithDetourData> generateInsertions(DrtRequest drtRequest, VehicleEntry vEntry) {
		int stopCount = vEntry.stops.size();
		List<InsertionWithDetourData> insertions = new ArrayList<>();

		DvrpLoad vehicleCapacity = vEntry.vehicle.getCapacity();
		// If the vehicle is currently at a CapacityChangeTask, it might not be already reflected in the vehicle's getCapacity().
		// We then retrieve the capacity from the task
		if(vEntry.start.task.isPresent() && vEntry.start.task.get() instanceof CapacityChangeTask capacityChangeTask) {
			vehicleCapacity = capacityChangeTask.getChangedCapacity();
		}

		// Since the vehicle capacity can change during the day. We need to check the load added by the request against all future capacities to be able to exit early.
		boolean compatibleWithOneCapacity = drtRequest.getLoad().fitsIn(vehicleCapacity);
		if (!compatibleWithOneCapacity) {
			for(Waypoint.Stop stop: vEntry.stops) {
				DvrpLoad changedCapacity = stop.getChangedCapacity();

				if(changedCapacity != null) {
					if(drtRequest.getLoad().fitsIn(changedCapacity)) {
						compatibleWithOneCapacity = true;
						break;
					}
				}
			}
		}
		if(!compatibleWithOneCapacity) {
			return Collections.emptyList();
		}

		DvrpLoad occupancy = vEntry.start.occupancy;

		for (int i = 0; i < stopCount; i++) {// insertions up to before last stop
			Waypoint.Stop nextStop = nextStop(vEntry, i);

			// (0) we first make sure that the request load is compatible with the capacity of the vehicle
			boolean allowed = drtRequest.getLoad().fitsIn(vehicleCapacity);

			// (1) only not fully loaded arcs
			// This is different than using allowed &= ... which causes the right hand expression to be always evaluated even if left variable is already false
			allowed = allowed && occupancy.add(drtRequest.getLoad()).fitsIn(vehicleCapacity);

			// (2) check if the request wants to depart after the departure time of the next
			// stop. We can early on filter out the current insertion, because we will
			// neither be able to insert our stop before the next stop nor merge the request
			// into it.
			allowed &= drtRequest.getEarliestStartTime() <= nextStop.getDepartureTime();

			if (allowed) {
				if (drtRequest.getFromLink() != nextStop.task.getLink()) {// next stop at different link
					generateDropoffInsertions(drtRequest, vEntry, i, insertions);
				} else {
					// this is the case where we insert a new request *before* a stop that is
					// on the same link as the pickup link. Initially, the reasoning was that the
					// new request will be merged *into* the existing task if all constraints hold,
					// i.e. the request will be appended. So only the insertion *after* this task is
					// necessary to evaluate. However, with prebooking, the situation is different:
					// if the next task is prebooked (in the future), we may want to insert another
					// task here on the same link (maybe a pickup followed by its dropoff) but much
					// earlier. In that case it is actually a valid insertion.

					boolean viableSameLink = vEntry.getPrecedingStayTime(i) > 0.0;
					if (viableSameLink && drtRequest.getEarliestStartTime() < nextStop.getArrivalTime()) {
						// the new request wants to depart before the start of the next stop, which may
						// be a viable insertion. Note that if the requested wanted to depart after the
						// start of the next stop, but before its end, this is a special case that is
						// covered further downstream as a special case of merging the pickup into the
						// existing stop task.

						generateDropoffInsertions(drtRequest, vEntry, i, insertions);
					}
				}
			}

			// update capacity
			vehicleCapacity = Objects.requireNonNullElse(nextStop.getChangedCapacity(), vehicleCapacity);

			occupancy = nextStop.outgoingOccupancy;
		}

		// here we still have to check if the load of the request is compatible with the last vehicle capacity
		if(drtRequest.getLoad().fitsIn(vehicleCapacity)) {
			generateDropoffInsertions(drtRequest, vEntry, stopCount, insertions);// at/after last stop
		}

		return insertions;
	}

	private void generateDropoffInsertions(DrtRequest request, VehicleEntry vEntry, int i,
			List<InsertionWithDetourData> insertions) {
		var pickupInsertion = createPickupInsertion(request, vEntry, i, true);
		double toPickupDepartureTime = pickupInsertion.previousWaypoint.getDepartureTime();
		double toPickupTT = detourTimeEstimator.estimateTime(pickupInsertion.previousWaypoint.getLink(),
				request.getFromLink(), toPickupDepartureTime);
		double earliestPickupStartTime = Math.max(toPickupDepartureTime + toPickupTT, request.getEarliestStartTime());
		double fromPickupTT = detourTimeEstimator.estimateTime(request.getFromLink(),
				pickupInsertion.nextWaypoint.getLink(),
				earliestPickupStartTime); //TODO stopDuration not included
		var pickupDetourInfo = detourTimeCalculator.calcPickupDetourInfo(vEntry, pickupInsertion, toPickupTT,
				fromPickupTT, true, request);

		if (i == 0 && !checkStartSlack(vEntry, request, pickupDetourInfo)) {
			// Inserting at schedule start and extending an ongoing stop task further than allowed
			return;
		}

		int stopCount = vEntry.stops.size();
		// i == j
		if (vEntry.getSlackTime(i) >= pickupDetourInfo.pickupTimeLoss) {
			// insertion: i -> pickup -> dropoff -> i+1 (only if time slack allows)
			int j = i;
			if (i == stopCount || request.getToLink() != nextStop(vEntry, j).task.getLink()) {
				// next stop at different link
				// otherwise, do not evaluate insertion _before_stop j, evaluate only insertion _after_ stop j
				addInsertion(insertions,
						createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT, pickupDetourInfo,
								j));
			} else {
				// special case: inserting dropoff before prebooked task on the same link
				// see the reasoning in generateInsertions

				boolean viableSameLink = vEntry.getPrecedingStayTime(j) > 0.0;
				if (viableSameLink && earliestPickupStartTime + fromPickupTT < nextStop(vEntry, j).getArrivalTime()) {
					addInsertion(insertions,
							createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT, pickupDetourInfo,
									j));
				}
			}
		}

		if (i == stopCount) {
			// insertion after last stop
			return;
		}

		//calculate it once for all j > i
		pickupInsertion = createPickupInsertion(request, vEntry, i, false);
		fromPickupTT = detourTimeEstimator.estimateTime(request.getFromLink(), pickupInsertion.nextWaypoint.getLink(),
				earliestPickupStartTime); //TODO stopDuration not included
		pickupDetourInfo = detourTimeCalculator.calcPickupDetourInfo(vEntry, pickupInsertion, toPickupTT, fromPickupTT,
				false, request);

		if (vEntry.getSlackTime(i) < pickupDetourInfo.pickupTimeLoss) {
			return; // skip all insertions: i -> pickup -> dropoff
		}

		DvrpLoad capacity = getVehicleCapacityAtStop(vEntry, i-1);

		for (int j = i + 1; j < stopCount; j++) {// insertions up to before last stop
			// i -> pickup -> i+1 && j -> dropoff -> j+1
			// check the capacity constraints if i < j (already validated for `i == j`)
			Waypoint.Stop currentStop = currentStop(vEntry, j);

			// update capacity
			capacity = Objects.requireNonNullElse(currentStop.getChangedCapacity(), capacity);

			if (!request.getLoad().fitsIn(capacity) || !currentStop.outgoingOccupancy.add(request.getLoad()).fitsIn(capacity)) {
				if (request.getToLink() == currentStop.task.getLink()) {
					//special case -- we can insert dropoff exactly at node j
					addInsertion(insertions,
							createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT,
									pickupDetourInfo, j));
				}

				return;// stop iterating -- cannot insert dropoff after node j
			}

			if (request.getToLink() != nextStop(vEntry, j).task.getLink()) {// next stop at different link
				//do not evaluate insertion _before_stop j, evaluate only insertion _after_ stop j
				addInsertion(insertions,
						createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT, pickupDetourInfo,
								j));
			} else {
				// special case: inserting dropoff before prebooked task on the same link
				// see the reasoning in generateInsertions

				boolean viableSameLink = vEntry.getPrecedingStayTime(j) > 0.0;
				if (viableSameLink && earliestPickupStartTime + fromPickupTT < nextStop(vEntry, j).getArrivalTime()) {
					addInsertion(insertions,
							createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT, pickupDetourInfo,
									j));
				}
			}
		}

		// if the last stop changes the vehicle's capacity, we'll need to take it into account
		capacity = Objects.requireNonNullElse(currentStop(vEntry, stopCount).getChangedCapacity(), capacity);

		if(request.getLoad().fitsIn(capacity)) {
			addInsertion(insertions,
				createInsertionWithDetourData(request, vEntry, pickupInsertion, fromPickupTT, pickupDetourInfo,
					stopCount));
		}
	}

	private void addInsertion(List<InsertionWithDetourData> insertions, InsertionWithDetourData insertion) {
		if (insertion != null) {
			insertions.add(insertion);
		}
	}

	private Waypoint.Stop currentStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx - 1);
	}

	private Waypoint.Stop nextStop(VehicleEntry entry, int insertionIdx) {
		return entry.stops.get(insertionIdx);
	}

	private boolean checkStartSlack(VehicleEntry vEntry, DrtRequest request, PickupDetourInfo pickupDetourInfo) {
		if (vEntry.start.task.isEmpty()) {
			return true;
		}

		Task startTask = vEntry.start.task.get();

		if (!DrtTaskBaseType.STOP.isBaseTypeOf(startTask)) {
			return true;
		}

		DrtStopTask stopTask = (DrtStopTask) startTask;

		if (stopTask.getLink() != request.getFromLink()) {
			return true;
		}

		return vEntry.getStartSlackTime() >= pickupDetourInfo.departureTime - stopTask.getEndTime();
	}

	private DvrpLoad getVehicleCapacityAtStop(VehicleEntry vEntry, int stopIndex) {
		DvrpLoad vehicleCapacity = vEntry.vehicle.getCapacity();

		for(int i = 0; i <= stopIndex; i++) {
			vehicleCapacity = Objects.requireNonNullElse(vEntry.stops.get(i).getChangedCapacity(), vehicleCapacity);
		}

		return vehicleCapacity;
	}

	private InsertionWithDetourData createInsertionWithDetourData(DrtRequest request, VehicleEntry vehicleEntry,
			InsertionPoint pickupInsertion, double fromPickupTT, PickupDetourInfo pickupDetourInfo, int dropoffIdx) {
		var dropoffInsertion = createDropoffInsertion(request, vehicleEntry, pickupInsertion, dropoffIdx);
		var insertion = new Insertion(vehicleEntry, pickupInsertion, dropoffInsertion, request.getLoad());

		double toDropoffDepartureTime = pickupInsertion.index == dropoffIdx ?
				pickupDetourInfo.departureTime :
				dropoffInsertion.previousWaypoint.getDepartureTime() + pickupDetourInfo.pickupTimeLoss;
		double toDropoffTT = pickupInsertion.index == dropoffIdx ?
				fromPickupTT :
				detourTimeEstimator.estimateTime(dropoffInsertion.previousWaypoint.getLink(), request.getToLink(),
						toDropoffDepartureTime);
		double fromDropoffTT = dropoffIdx == vehicleEntry.stops.size() ?
				0 :
				detourTimeEstimator.estimateTime(request.getToLink(), dropoffInsertion.nextWaypoint.getLink(),
						toDropoffDepartureTime + toDropoffTT); //TODO stopDuration not included

		var dropoffDetourInfo = detourTimeCalculator.calcDropoffDetourInfo(insertion, toDropoffTT, fromDropoffTT,
				pickupDetourInfo, request);

		if (vehicleEntry.getSlackTime(dropoffIdx)
				< pickupDetourInfo.pickupTimeLoss + dropoffDetourInfo.dropoffTimeLoss) {
			return null; // skip this dropoff insertion
		}

		return new InsertionWithDetourData(insertion, null, new DetourTimeInfo(pickupDetourInfo, dropoffDetourInfo));
	}
}
