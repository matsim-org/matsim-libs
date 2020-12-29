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
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final DvrpVehicle vehicle;
		public final Waypoint.Start start;
		public final ImmutableList<Waypoint.Stop> stops;
		public final Waypoint.End end;

		public Entry(DvrpVehicle vehicle, Waypoint.Start start, ImmutableList<Waypoint.Stop> stops) {
			this.vehicle = vehicle;
			this.start = start;
			this.stops = stops;
			this.end = Waypoint.End.OPEN_END;
		}

		public Waypoint getWaypoint(int index) {
			return index == 0 ? start : (index == stops.size() + 1 ? end : stops.get(index - 1));
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
