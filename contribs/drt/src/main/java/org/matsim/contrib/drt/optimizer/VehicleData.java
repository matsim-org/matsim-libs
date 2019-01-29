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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final Vehicle vehicle;
		public final LinkTimePair start;
		public final int startOccupancy;
		public final ImmutableList<Stop> stops;

		public Entry(Vehicle vehicle, LinkTimePair start, int startOccupancy, ImmutableList<Stop> stops) {
			this.vehicle = vehicle;
			this.start = start;
			this.startOccupancy = startOccupancy;
			this.stops = stops;
		}
	}

	public static class Stop {
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

		private double calcLatestArrivalTime() {
			return getMaxTimeConstraint(
					task.getDropoffRequests().stream().mapToDouble(DrtRequest::getLatestArrivalTime),
					task.getBeginTime());
		}

		private double calcLatestDepartureTime() {
			return getMaxTimeConstraint(task.getPickupRequests().stream().mapToDouble(DrtRequest::getLatestStartTime),
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
		Entry create(Vehicle vehicle, double currentTime);
	}

	private final double currentTime;
	private final EntryFactory entryFactory;
	private final Map<Id<Vehicle>, Entry> entries;

	public VehicleData(double currentTime, Stream<? extends Vehicle> vehicles, EntryFactory entryFactory,
			ForkJoinPool forkJoinPool) {
		this.currentTime = currentTime;
		this.entryFactory = entryFactory;
		try {
			entries = forkJoinPool.submit(() -> vehicles.parallel()//
					.map(v -> entryFactory.create(v, currentTime))//
					.filter(Objects::nonNull)//
					.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e)))//
					.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateEntry(Vehicle vehicle) {
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
