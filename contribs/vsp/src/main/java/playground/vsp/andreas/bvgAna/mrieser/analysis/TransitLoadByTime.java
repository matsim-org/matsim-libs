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

package playground.vsp.andreas.bvgAna.mrieser.analysis;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the number of passenger that are in a transit vehicle as
 * a function of time. Requires that the events come sorted by time.
 *
 * @author mrieser
 */
public class TransitLoadByTime implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final ConcurrentHashMap<Id<Vehicle>, VehicleData> vehicleData = new ConcurrentHashMap<>();

	public int getVehicleLoad(final Id<Vehicle> vehicleId, final double time) {
		VehicleData vData = getVehicleData(vehicleId, false);
		if (vData == null) {
			return 0;
		}
		return vData.getPassengerCount(time);
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		VehicleData vData = getVehicleData(event.getVehicleId(), true);
		vData.addPassengerChange(event.getTime(), +1);
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		VehicleData vData = getVehicleData(event.getVehicleId(), true);
		vData.addPassengerChange(event.getTime(), -1);
	}

	@Override
	public void reset(int iteration) {
		this.vehicleData.clear();
	}

	private VehicleData getVehicleData(final Id<Vehicle> vehicleId, final boolean createIfMissing) {
		VehicleData vData = this.vehicleData.get(vehicleId);
		if (vData == null && createIfMissing) {
			// optimization: only allocate new object when not found
			VehicleData newData = new VehicleData();
			vData = this.vehicleData.putIfAbsent(vehicleId, newData);
			if (vData == null) {
				vData = newData;
			}
		}
		return vData;
	}

	private static class VehicleData {
		public final TreeMap<Double, Integer> nOfPassengersByTime = new TreeMap<>(); // Time, nOfPassengers

		public VehicleData() {
		}

		public void addPassengerChange(final double time, final int delta) {
			Integer i = this.nOfPassengersByTime.get(time);
			if (i == null) {
				Map.Entry<Double, Integer> prev = this.nOfPassengersByTime.floorEntry(time);
				if (prev == null) {
					this.nOfPassengersByTime.put(time, delta);
				} else {
					this.nOfPassengersByTime.put(time, prev.getValue() + delta);
				}
			} else {
				this.nOfPassengersByTime.put(time, i + delta);
			}
		}

		public int getPassengerCount(final double time) {
			Map.Entry<Double, Integer> floor = this.nOfPassengersByTime.floorEntry(time);
			if (floor == null) {
				return 0;
			}
			return floor.getValue();
		}

	}
}
