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

package org.matsim.contrib.drt.optimizer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.collect.ImmutableList;
import com.sun.istack.Nullable;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final DvrpVehicle vehicle;
		public final Start start;
		public final ImmutableList<Stop> stops;
		private final End end;//TODO keep it private until it is used in insertion cost calculation etc.

		public Entry(DvrpVehicle vehicle, Start start, ImmutableList<Stop> stops) {
			this.vehicle = vehicle;
			this.start = start;
			this.stops = stops;
			this.end = End.OPEN_END;
		}

		//TODO allow index == stops.size() ==> return end ???
		public Waypoint getWaypoint(int index) {
			return index == 0 ? start : stops.get(index - 1);
		}
	}

	public interface Waypoint {
		Link getLink();

		double getDepartureTime();

		int getOutgoingOccupancy();
	}

	public static class Start implements Waypoint {
		@Nullable // if schedule status is PLANNED
		public final Task task;
		public final Link link;
		public final double time;
		public final int occupancy;

		public Start(Task task, Link link, double time, int occupancy) {
			this.task = task;
			this.link = link;
			this.time = time;
			this.occupancy = occupancy;
		}

		@Override
		public Link getLink() {
			return link;
		}

		@Override
		public double getDepartureTime() {
			return time;
		}

		@Override
		public int getOutgoingOccupancy() {
			return occupancy;
		}
	}

	public static class End implements Waypoint {
		public static End OPEN_END = new End();

		@Nullable
		public final Link link;//null if open-end route
		public final OptionalTime time;//undefined if open-end route

		private End() {
			link = null;
			time = OptionalTime.undefined();
		}

		public End(Link link, double time) {
			this.link = link;
			this.time = OptionalTime.defined(time);
		}

		public boolean isOpenEnd() {
			return link == null;
		}

		@Override
		@Nullable
		public Link getLink() {
			return link;
		}

		@Override
		public double getDepartureTime() {
			return time.seconds();
		}

		@Override
		public int getOutgoingOccupancy() {
			throw new UnsupportedOperationException("End is the terminal waypoint");
		}
	}

	public static class Stop implements Waypoint {
		public final DrtStopTask task;
		public final double latestArrivalTime;// relating to max passenger drive time (for dropoff requests)
		public final double latestDepartureTime;// relating to passenger max wait time (for pickup requests)
		public final int occupancyChange;// diff in pickups and dropoffs
		public final int outgoingOccupancy;

		public Stop(DrtStopTask task, int outputOccupancy) {
			this.task = task;
			this.outgoingOccupancy = outputOccupancy;

			latestArrivalTime = calcLatestArrivalTime();
			// essentially the min of the latest possible arrival times at this stop

			latestDepartureTime = calcLatestDepartureTime();
			// essentially the min of the latest possible pickup times at this stop

			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		@Override
		public Link getLink() {
			return task.getLink();
		}

		@Override
		public double getDepartureTime() {
			return task.getEndTime();
		}

		@Override
		public int getOutgoingOccupancy() {
			return outgoingOccupancy;
		}

		private double calcLatestArrivalTime() {
			return getMaxTimeConstraint(
					task.getDropoffRequests().values().stream().mapToDouble(DrtRequest::getLatestArrivalTime),
					task.getBeginTime());
		}

		private double calcLatestDepartureTime() {
			return getMaxTimeConstraint(
					task.getPickupRequests().values().stream().mapToDouble(DrtRequest::getLatestStartTime),
					task.getEndTime());
		}

		private double getMaxTimeConstraint(DoubleStream latestAllowedTimes, double scheduledTime) {
			//XXX if task is already delayed beyond one or more of latestTimes, use scheduledTime as maxTime constraint
			//thus we can still add a new request to the already scheduled stops (as no further delays are incurred)
			//but we cannot add a new stop before the delayed task
			return Math.max(latestAllowedTimes.min().orElse(Double.MAX_VALUE), scheduledTime);
		}

		@Override
		public String toString() {
			return "VehicleData.Stop for: " + task.toString();
		}
	}

	public interface EntryFactory {
		Entry create(DvrpVehicle vehicle, double currentTime);
	}

	private final double currentTime;
	private final EntryFactory entryFactory;
	private final Map<Id<DvrpVehicle>, Entry> entries;

	public VehicleData(double currentTime, Stream<? extends DvrpVehicle> vehicles, EntryFactory entryFactory,
			ForkJoinPool forkJoinPool) {
		this.currentTime = currentTime;
		this.entryFactory = entryFactory;
		entries = forkJoinPool.submit(() -> vehicles.parallel()
				.map(v -> entryFactory.create(v, currentTime))
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e))).join();
	}

	public void updateEntry(DvrpVehicle vehicle) {
		Entry e = entryFactory.create(vehicle, currentTime);
		if (e != null) {
			entries.put(vehicle.getId(), e);
		} else {
			entries.remove(vehicle.getId());
		}
	}

	public int getSize() {
		return entries.size();
	}

	public Collection<Entry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
}
