/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.features.MobsimFeature;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

public class TransitFeature implements MobsimFeature {

	private final TransitStopAgentTracker agentTracker;
	private final PriorityQueue<TimeRecord> vehiclesAtStop = new PriorityQueue<TimeRecord>();
	private final HashMap<Double, TimeRecord> timeLookup = new HashMap<Double, TimeRecord>();

	public TransitFeature(final TransitStopAgentTracker agentTracker) {
		this.agentTracker = agentTracker;
	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	@Override
	public void beforeMobSim() {
		this.vehiclesAtStop.clear();
		this.timeLookup.clear();
	}

	@Override
	public void doSimStep(double time) {
		TimeRecord peek = this.vehiclesAtStop.peek();
		if (peek == null) {
			return;
		}
		if (peek.time <= time) {
			this.vehiclesAtStop.poll();
			for (VehicleRecord r : peek.list) {
				r.driver.handleNextAction(r.link, time);
			}
		}
	}

	@Override
	public void afterMobSim() {
	}

	public void vehicleAtStop(final MobsimVehicle vehicle, final TransitRouteStop stop, final double nextCheckTime, final MobsimLink link, final TransitDriverAgent driver) {
		TimeRecord record = this.timeLookup.get(nextCheckTime);
		if (record == null) {
			record = new TimeRecord(nextCheckTime);
			this.timeLookup.put(nextCheckTime, record);
			this.vehiclesAtStop.add(record);
		}
		record.list.add(new VehicleRecord(vehicle, stop, link, driver));
	}

	private static class TimeRecord implements Comparable<TimeRecord> {
		private final double time;
		/*package*/ final List<VehicleRecord> list = new ArrayList<VehicleRecord>();

		public TimeRecord(final double time) {
			this.time = time;
		}

		@Override
		public int compareTo(TimeRecord o) {
			return Double.compare(this.time, o.time);
		}
	}

	private static class VehicleRecord {
		/*package*/ final MobsimVehicle vehicle;
		/*package*/ final TransitRouteStop stop;
		/*package*/ final MobsimLink link;
		/*package*/ final TransitDriverAgent driver;

		public VehicleRecord(final MobsimVehicle vehicle, final TransitRouteStop stop, final MobsimLink link, final TransitDriverAgent driver) {
			this.vehicle = vehicle;
			this.stop = stop;
			this.link = link;
			this.driver = driver;
		}
	}
}
