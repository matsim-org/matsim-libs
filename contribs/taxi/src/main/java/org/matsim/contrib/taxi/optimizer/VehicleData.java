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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry extends LinkTimePair {
		public final int idx;
		public final Vehicle vehicle;
		public final boolean idle;

		public Entry(int idx, Vehicle vehicle, Link link, double time, boolean idle) {
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

	public VehicleData(Iterable<Entry> vehEntries) {
		int idx = 0;
		int idleCounter = 0;
		for (Entry e : vehEntries) {
			entries.add(new Entry(idx++, e.vehicle, e.link, e.time, e.idle));
			if (e.idle) {
				idleCounter++;
			}
		}

		idleCount = idleCounter;
	}

	public VehicleData(TaxiOptimizerContext optimContext, Iterable<? extends Vehicle> vehicles) {
		this(optimContext, vehicles, NO_PLANNING_HORIZON);
	}

	// skipping vehicles with departure.time > curr_time + maxDepartureDelay
	public VehicleData(TaxiOptimizerContext optimContext, Iterable<? extends Vehicle> vehicles,
			double planningHorizon) {
		double currTime = optimContext.timer.getTimeOfDay();
		double maxDepartureTime = currTime + planningHorizon;
		TaxiScheduleInquiry scheduleInquiry = optimContext.scheduler;

		int idx = 0;
		int idleCounter = 0;
		for (Vehicle v : vehicles) {
			LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(v);

			if (departure != null && departure.time <= maxDepartureTime) {
				boolean idle = departure.time == currTime // to avoid unnecessary calls to Scheduler.isIdle()
						&& scheduleInquiry.isIdle(v);
				entries.add(new Entry(idx++, v, departure.link, departure.time, idle));
				if (idle) {
					idleCounter++;
				}
			}
		}

		idleCount = idleCounter;
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
