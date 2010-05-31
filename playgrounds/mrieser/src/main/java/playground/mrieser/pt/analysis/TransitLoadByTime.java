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

package playground.mrieser.pt.analysis;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Calculates the number of passenger that are in a transit vehicle as
 * a function of time. Requires that the events come sorted by time.
 *
 * @author mrieser
 */
public class TransitLoadByTime implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private Map<Id, VehicleData> vehicleData = new ConcurrentHashMap<Id, VehicleData>();

	public int getVehicleLoad(final Id vehicleId, final double time) {
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

	private VehicleData getVehicleData(final Id vehicleId, final boolean createIfMissing) {
		VehicleData vData = this.vehicleData.get(vehicleId);
		if (vData == null) {
			synchronized(this.vehicleData) { // putIfMissing
				vData = this.vehicleData.get(vehicleId);
				if (vData == null) {
					vData = new VehicleData();
					this.vehicleData.put(vehicleId, vData);
				}
			}
		}
		return vData;
	}

	private static class VehicleData {
		public final TreeMap<Double, Integer> nOfPassengersByTime = new TreeMap<Double, Integer>(); // Time, nOfPassengers

		public VehicleData() {
		}

		public void addPassengerChange(final double time, final int delta) {
			Integer i = this.nOfPassengersByTime.get(time);
			if (i == null) {
				Map.Entry<Double, Integer> prev = this.nOfPassengersByTime.floorEntry(time);
				if (prev == null) {
					this.nOfPassengersByTime.put(time, delta);
				} else {
					this.nOfPassengersByTime.put(time, prev.getValue().intValue() + delta);
				}
			} else {
				this.nOfPassengersByTime.put(time, i.intValue() + delta);
			}
		}

		public int getPassengerCount(final double time) {
			Map.Entry<Double, Integer> floor = this.nOfPassengersByTime.floorEntry(time);
			if (floor == null) {
				return 0;
			}
			return floor.getValue().intValue();
		}

	}
}
