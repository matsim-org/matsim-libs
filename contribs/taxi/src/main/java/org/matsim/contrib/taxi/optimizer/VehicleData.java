/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry extends LinkTimePair {
		public final int idx;
		public final DvrpVehicle vehicle;
		public final boolean idle;

		public Entry(int idx, DvrpVehicle vehicle, Link link, double time, boolean idle) {
			super(link, time);
			this.idx = idx;
			this.vehicle = vehicle;
			this.idle = idle;
		}
	}

	// max 48 hours of departure delay (== not a real constraint)
	private static final double NO_PLANNING_HORIZON = 2 * 24 * 3600;

	private final List<Entry> entries = new ArrayList<>();
	private final int idleCount;

	public VehicleData(double currentTime, TaxiScheduleInquiry scheduleInquiry,
			Stream<? extends DvrpVehicle> vehicles) {
		this(currentTime, scheduleInquiry, vehicles, NO_PLANNING_HORIZON);
	}

	// skipping vehicles with departure.time > curr_time + maxDepartureDelay
	public VehicleData(double currentTime, TaxiScheduleInquiry scheduleInquiry, Stream<? extends DvrpVehicle> vehicles,
			double planningHorizon) {
		double maxDepartureTime = currentTime + planningHorizon;

		MutableInt idx = new MutableInt();
		MutableInt idleCounter = new MutableInt();
		vehicles.forEach(v -> {
			LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(v);

			if (departure != null && departure.time <= maxDepartureTime) {
				boolean idle = scheduleInquiry.isIdle(v);
				entries.add(new Entry(idx.getAndIncrement(), v, departure.link, departure.time, idle));
				if (idle) {
					idleCounter.increment();
				}
			}
		});

		idleCount = idleCounter.intValue();
	}

	public int getSize() {
		return entries.size();
	}

	public Entry getEntry(int idx) {
		return entries.get(idx);
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public int getIdleCount() {
		return idleCount;
	}
}
