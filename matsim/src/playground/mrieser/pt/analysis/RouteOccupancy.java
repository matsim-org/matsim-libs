/* *********************************************************************** *
 * project: org.matsim.*
 * RouteOccupancy.java
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;

/**
 * Keeps track of the total number of passengers entering and leaving a transit
 * vehicle along a given route, on all Departures.
 *
 * @author mrieser
 */
public class RouteOccupancy implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static final Integer Int1 = Integer.valueOf(1);

	private final TransitRoute transitRoute;
	private final VehicleTracker tracker;
	private Set<Id> routeVehicles = null;
	private final Map<Id, Integer> enteringPassengers = new HashMap<Id, Integer>();
	private final Map<Id, Integer> leavingPassengers = new HashMap<Id, Integer>();

	public RouteOccupancy(final TransitRoute transitRoute, final VehicleTracker tracker) {
		this.transitRoute = transitRoute;
		this.tracker = tracker;
	}

	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.routeVehicles == null) {
			collectVehiclesInfo();
		}
		if (this.routeVehicles.contains(event.getVehicleId())) {
			Id facilityId = this.tracker.getFacilityIdForVehicle(event.getVehicleId());
			Integer count = this.enteringPassengers.get(facilityId);
			if (count == null) {
				this.enteringPassengers.put(facilityId, Int1);
			} else {
				this.enteringPassengers.put(facilityId, Integer.valueOf(1 + count.intValue()));
			}
		}
	}

	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.routeVehicles == null) {
			collectVehiclesInfo();
		}
		if (this.routeVehicles.contains(event.getVehicleId())) {
			Id facilityId = this.tracker.getFacilityIdForVehicle(event.getVehicleId());
			Integer count = this.leavingPassengers.get(facilityId);
			if (count == null) {
				this.leavingPassengers.put(facilityId, Int1);
			} else {
				this.leavingPassengers.put(facilityId, Integer.valueOf(1 + count.intValue()));
			}
		}
	}

	public void reset(final int iteration) {
		this.routeVehicles = null;
		this.enteringPassengers.clear();
		this.leavingPassengers.clear();
	}

	public int getNumberOfEnteringPassengers(final Id stopFacilityId) {
		Integer count = this.enteringPassengers.get(stopFacilityId);
		if (count == null) {
			return 0;
		}
		return count.intValue();
	}

	public int getNumberOfLeavingPassengers(final Id stopFacilityId) {
		Integer count = this.leavingPassengers.get(stopFacilityId);
		if (count == null) {
			return 0;
		}
		return count.intValue();
	}

	/**
	 * Lazy initialization, as the vehicle info may not be available from the beginning.
	 */
	private void collectVehiclesInfo() {
		Set<Id> set = new HashSet<Id>(this.transitRoute.getDepartures().size()*2);

		for (Departure departure : this.transitRoute.getDepartures().values()) {
			if (departure.getVehicleId() != null) {
				set.add(departure.getVehicleId());
			}
		}

		/* try to make it thread-safe by assigning class-member at the end.
		 * if two threads enter this method, nothing bad should happen,
		 * as both threads should generated the same initialization.
		 */
		this.routeVehicles = set;
	}

}
