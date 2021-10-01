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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the number of people waiting at a transit stop facility as
 * a function of time.
 *
 * @author mrieser
 */
public class TransitStopLoadByTime implements ActivityEndEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehicleFacilityMap = new HashMap<>();
	private final ConcurrentHashMap<Id<Person>, Double> passengerWaitingSince = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Id<TransitStopFacility>, StopData> stopData = new ConcurrentHashMap<>();

	public int getStopFacilityLoad(final Id<TransitStopFacility> stopFacilityId, final double time) {
		StopData sData = getStopData(stopFacilityId, false);
		if (sData == null) {
			return 0;
		}
		return sData.getWaitingCount(time);
	}

	public Map<Double, Integer> getStopFacilityLoad(final Id<TransitStopFacility> stopFacilityId) {
		StopData sData = getStopData(stopFacilityId, false);
		if (sData == null) {
			return null;
		}
		return Collections.unmodifiableMap(sData.getWaitingCount());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(event.getActType())) {
			this.passengerWaitingSince.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		Id<TransitStopFacility> stopId = this.vehicleFacilityMap.get(event.getVehicleId());
		Double waitStartTime = this.passengerWaitingSince.get(event.getPersonId());
		StopData sData = getStopData(stopId, true);
		sData.addWaitingChange(waitStartTime, +1);
		sData.addWaitingChange(event.getTime(), -1);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleFacilityMap.put(event.getVehicleId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleFacilityMap.remove(event.getVehicleId());
	}

	@Override
	public void reset(int iteration) {
		this.stopData.clear();
		this.passengerWaitingSince.clear();
	}

	private StopData getStopData(final Id<TransitStopFacility> stopId, final boolean createIfMissing) {
		StopData sData = this.stopData.get(stopId);
		if (sData == null && createIfMissing) {
			StopData newData = new StopData();
			sData = this.stopData.putIfAbsent(stopId, newData);
			if (sData == null) {
				sData = newData;
			}
		}
		return sData;
	}

	private static class StopData {
		private final SortedMap<Double, Integer> nOfPassengersDeltaByTime = new TreeMap<>(); // Time, nOfDeltaPassengers
		private volatile TreeMap<Double, Integer> nOfPassengersByTime = null;

		public StopData() {
		}

		public void addWaitingChange(final double time, final int delta) {
			this.nOfPassengersDeltaByTime.merge(time, delta, Integer::sum);
			this.nOfPassengersByTime = null;
		}

		public int getWaitingCount(final double time) {
			NavigableMap<Double, Integer> map = this.nOfPassengersByTime;
			if (map == null) {
				map = calculateWaitingLoad();
			}

			Map.Entry<Double, Integer> floor = map.floorEntry(time);
			if (floor == null) {
				return 0;
			}
			return floor.getValue();
		}

		private NavigableMap<Double, Integer> calculateWaitingLoad() {
			TreeMap<Double, Integer> map = new TreeMap<>();
			int count = 0;
			for (Map.Entry<Double, Integer> e : this.nOfPassengersDeltaByTime.entrySet()) {
				count += e.getValue();
				map.put(e.getKey(), count);
			}
			this.nOfPassengersByTime = map;
			return map;
		}

		/*protected*/ Map<Double, Integer> getWaitingCount() {
			SortedMap<Double, Integer> map = this.nOfPassengersByTime;
			if (map == null) {
				return calculateWaitingLoad();
			}
			return map;
		}

	}
}
