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
import java.util.stream.Collectors;
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
		public final double maxArrivalTime;// relating to max pass drive time (for dropoff requests)
		public final double maxDepartureTime;// relating to pass max wait time (for pickup requests)
		public final int occupancyChange;// diff in pickups and dropoffs
		public final int outgoingOccupancy;

		public Stop(DrtStopTask task, int outputOccupancy) {
			this.task = task;
			this.outgoingOccupancy = outputOccupancy;

			maxArrivalTime = calcMaxArrivalTime();
			maxDepartureTime = calcMaxDepartureTime();
			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		private double calcMaxArrivalTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getDropoffRequests()) {
				double reqMaxArrivalTime = r.getLatestArrivalTime();
				if (reqMaxArrivalTime < maxTime) {
					maxTime = reqMaxArrivalTime;
				}
			}
			return maxTime;
		}

		private double calcMaxDepartureTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getPickupRequests()) {
				double reqMaxDepartureTime = r.getLatestStartTime();
				if (reqMaxDepartureTime < maxTime) {
					maxTime = reqMaxDepartureTime;
				}
			}
			return maxTime;
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

	public VehicleData(double currentTime, Stream<? extends Vehicle> vehicles, EntryFactory entryFactory) {
		this.currentTime = currentTime;
		this.entryFactory = entryFactory;
		entries = vehicles.map(v -> entryFactory.create(v, currentTime)).filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e));
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