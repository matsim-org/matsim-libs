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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.PtConstants;

/**
 * Calculates the number of people waiting at a transit stop facility as
 * a function of time.
 *
 * @author mrieser
 */
public class TransitStopLoadByTime implements ActivityEndEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final Map<Id, Id> vehicleFacilityMap = new HashMap<Id, Id>();
	private Map<Id, Double> passengerWaitingSince = new ConcurrentHashMap<Id, Double>();
	private Map<Id, StopData> stopData = new ConcurrentHashMap<Id, StopData>();

	public int getStopFacilityLoad(final Id stopFacilityId, final double time) {
		StopData sData = getStopData(stopFacilityId, false);
		if (sData == null) {
			return 0;
		}
		return sData.getWaitingCount(time);
	}

	public Map<Double, Integer> getStopFacilityLoad(final Id stopFacilityId) {
		StopData sData = getStopData(stopFacilityId, false);
		if (sData == null) {
			return null;
		}
		return Collections.unmodifiableMap(sData.nOfPassengersByTime);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(event.getActType())) {
			this.passengerWaitingSince.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		Id stopId = this.vehicleFacilityMap.get(event.getVehicleId());
		Double waitStartTime = this.passengerWaitingSince.get(event.getPersonId());
		StopData sData = getStopData(stopId, true);
		sData.addWaitingChange(waitStartTime.doubleValue(), +1);
		sData.addWaitingChange(event.getTime(), -1);
	}

	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleFacilityMap.put(event.getVehicleId(), event.getFacilityId());
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleFacilityMap.remove(event.getVehicleId());
	}

	public void reset(int iteration) {
		this.stopData.clear();
		this.passengerWaitingSince.clear();
	}

	private StopData getStopData(final Id stopId, final boolean createIfMissing) {
		StopData sData = this.stopData.get(stopId);
		if (sData == null) {
			synchronized(this.stopData) { // putIfMissing
				sData = this.stopData.get(stopId);
				if (sData == null) {
					sData = new StopData();
					this.stopData.put(stopId, sData);
				}
			}
		}
		return sData;
	}

	private static class StopData {
		public final TreeMap<Double, Integer> nOfPassengersByTime = new TreeMap<Double, Integer>(); // Time, nOfPassengers

		public StopData() {
		}

		public void addWaitingChange(final double time, final int delta) {
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

		public int getWaitingCount(final double time) {
			Map.Entry<Double, Integer> floor = this.nOfPassengersByTime.floorEntry(time);
			if (floor == null) {
				return 0;
			}
			return floor.getValue().intValue();
		}

	}
}
