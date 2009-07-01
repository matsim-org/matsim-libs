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

package playground.marcel.pt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.events.BasicPersonEntersVehicleEvent;
import org.matsim.core.basic.v01.events.BasicPersonLeavesVehicleEvent;
import org.matsim.core.basic.v01.events.handlers.BasicPersonEntersVehicleEventHandler;
import org.matsim.core.basic.v01.events.handlers.BasicPersonLeavesVehicleEventHandler;
import org.matsim.core.utils.misc.Time;

import playground.marcel.pt.transitSchedule.DepartureImpl;
import playground.marcel.pt.transitSchedule.TransitRouteImpl;
import playground.marcel.pt.transitSchedule.TransitRouteStopImpl;

public class TransitRouteAccessEgressAnalysis implements BasicPersonEntersVehicleEventHandler, BasicPersonLeavesVehicleEventHandler {

	public final TransitRouteImpl transitRoute;
	public Map<Id, DepartureImpl> headings = null;
	private Map<DepartureImpl, Map<Id, Integer>> accessCounters = new LinkedHashMap<DepartureImpl, Map<Id, Integer>>();
	private Map<DepartureImpl, Map<Id, Integer>> egressCounters = new LinkedHashMap<DepartureImpl, Map<Id, Integer>>();
	private final VehicleTracker vehTracker;

	public TransitRouteAccessEgressAnalysis(final TransitRouteImpl transitRoute, final VehicleTracker vehicleTracker) {
		this.transitRoute = transitRoute;
		this.vehTracker = vehicleTracker;
	}
	
	public void handleEvent(BasicPersonEntersVehicleEvent event) {
		if (this.headings == null) {
			collectHeadingsInfo();
		}
		DepartureImpl dep = this.headings.get(event.getVehicleId());
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

	public void handleEvent(BasicPersonLeavesVehicleEvent event) {
		if (this.headings == null) {
			collectHeadingsInfo();
		}
		DepartureImpl dep = this.headings.get(event.getVehicleId());
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

	public void reset(int iteration) {
		this.headings = null;
		this.accessCounters.clear();
		this.egressCounters.clear();
	}
	
	public void printStats() {
		List<Id> stopFacilityIds = new ArrayList<Id>(this.transitRoute.getStops().size());
		for (TransitRouteStopImpl stop : this.transitRoute.getStops()) {
			stopFacilityIds.add(stop.getStopFacility().getId());
		}
		
		System.out.print("stops/departure");
		for (Id id : stopFacilityIds) {
			System.out.print("\t" + id);
		}
		System.out.println();
		for (DepartureImpl departure : this.headings.values()) {
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
	
	public Map<Id, Integer> getAccessCounter(final DepartureImpl departure) {
		Map<Id, Integer> counter = this.accessCounters.get(departure);
		if (counter == null) {
			counter = new HashMap<Id, Integer>();
			this.accessCounters.put(departure, counter);
		}
		return counter;
	}
	
	public Map<Id, Integer> getEgressCounter(final DepartureImpl departure) {
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
		Map<Id, DepartureImpl> map = new HashMap<Id, DepartureImpl>(this.transitRoute.getDepartures().size()*2);
		
		for (DepartureImpl departure : this.transitRoute.getDepartures().values()) {
			if (departure.getVehicle() != null) {
				map.put(departure.getVehicle().getId(), departure);
			}
		}
		
		/* try to make it thread-safe by assigning class-member at the end.
		 * if two threads enter this method, nothing bad should happen,
		 * as both threads should generated the same initialization.
		 */
		this.headings = map;
	}

}
