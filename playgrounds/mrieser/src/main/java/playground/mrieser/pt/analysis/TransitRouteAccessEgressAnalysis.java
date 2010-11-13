/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteAccessEgressAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * Keeps track of the number of passengers entering and leaving each single
 * departure along a given route.
 *
 * @author mrieser
 */
public class TransitRouteAccessEgressAnalysis implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	public final TransitRoute transitRoute;
	public Map<Id, Departure> headings = null;
	private final Map<Departure, Map<Id, Integer>> accessCounters = new LinkedHashMap<Departure, Map<Id, Integer>>();
	private final Map<Departure, Map<Id, Integer>> egressCounters = new LinkedHashMap<Departure, Map<Id, Integer>>();
	private final VehicleTracker vehTracker;

	public TransitRouteAccessEgressAnalysis(final TransitRoute transitRoute, final VehicleTracker vehicleTracker) {
		this.transitRoute = transitRoute;
		this.vehTracker = vehicleTracker;
	}

	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.headings == null) {
			collectHeadingsInfo();
		}
		Departure dep = this.headings.get(event.getVehicleId());
		if (dep != null) {
			Id fId = this.vehTracker.getFacilityIdForVehicle(event.getVehicleId());
			Map<Id, Integer> counter = getAccessCounter(dep);
			Integer count = counter.get(fId);
			if (count == null) {
				counter.put(fId, Integer.valueOf(1));
			} else {
				counter.put(fId, Integer.valueOf(1 + count.intValue()));
			}
		}
	}

	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.headings == null) {
			collectHeadingsInfo();
		}
		Departure dep = this.headings.get(event.getVehicleId());
		if (dep != null) {
			Id fId = this.vehTracker.getFacilityIdForVehicle(event.getVehicleId());
			Map<Id, Integer> counter = getEgressCounter(dep);
			Integer count = counter.get(fId);
			if (count == null) {
				counter.put(fId, Integer.valueOf(1));
			} else {
				counter.put(fId, Integer.valueOf(1 + count.intValue()));
			}
		}
	}

	public void reset(final int iteration) {
		this.headings = null;
		this.accessCounters.clear();
		this.egressCounters.clear();
	}

	public void printStats() {
		List<Id> stopFacilityIds = new ArrayList<Id>(this.transitRoute.getStops().size());
		for (TransitRouteStop stop : this.transitRoute.getStops()) {
			stopFacilityIds.add(stop.getStopFacility().getId());
		}

		System.out.print("stops/departure");
		for (Id id : stopFacilityIds) {
			System.out.print("\t" + id);
		}
		System.out.println();
		for (Departure departure : this.headings.values()) {
			System.out.print(Time.writeTime(departure.getDepartureTime()));
			Map<Id, Integer> accessCounter = getAccessCounter(departure);
			for (Id id : stopFacilityIds) {
				Integer value = accessCounter.get(id);
				System.out.print("\t");
				if (value != null) {
					System.out.print(value.toString());
				} else {
					System.out.print("0");
				}
			}
			System.out.println();
			Map<Id, Integer> egressCounter = getEgressCounter(departure);
			for (Id id : stopFacilityIds) {
				Integer value = egressCounter.get(id);
				System.out.print("\t");
				if (value != null) {
					System.out.print(value.toString());
				} else {
					System.out.print("0");
				}
			}
			System.out.println();
		}
	}

	public Map<Id, Integer> getAccessCounter(final Departure departure) {
		Map<Id, Integer> counter = this.accessCounters.get(departure);
		if (counter == null) {
			counter = new HashMap<Id, Integer>();
			this.accessCounters.put(departure, counter);
		}
		return counter;
	}

	public Map<Id, Integer> getEgressCounter(final Departure departure) {
		Map<Id, Integer> counter = this.egressCounters.get(departure);
		if (counter == null) {
			counter = new HashMap<Id, Integer>();
			this.egressCounters.put(departure, counter);
		}
		return counter;
	}

	/**
	 * Lazy initialization, as the vehicle info may not be available from the beginning.
	 */
	private void collectHeadingsInfo() {
		Map<Id, Departure> map = new HashMap<Id, Departure>(this.transitRoute.getDepartures().size()*2);

		for (Departure departure : this.transitRoute.getDepartures().values()) {
			if (departure.getVehicleId() != null) {
				map.put(departure.getVehicleId(), departure);
			}
		}

		/* try to make it thread-safe by assigning class-member at the end.
		 * if two threads enter this method, nothing bad should happen,
		 * as both threads should generated the same initialization.
		 */
		this.headings = map;
	}

}
