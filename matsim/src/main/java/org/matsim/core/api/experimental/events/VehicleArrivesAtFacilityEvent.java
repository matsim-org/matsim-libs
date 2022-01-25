/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleArrivesAtFacilityEvent.java
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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 * 
 * Should be replaced by some more generic VehicleArrivalEvent
 * which supports both links and facilities.
 */
public final class VehicleArrivesAtFacilityEvent extends Event {

	public static final String EVENT_TYPE = "VehicleArrivesAtFacility";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String ATTRIBUTE_DELAY = "delay";

	private final Id<Vehicle> vehicleId;
	private final Id<TransitStopFacility> facilityId;
	private double delay;

	public VehicleArrivesAtFacilityEvent(final double time, final Id<Vehicle> vehicleId, 
			final Id<TransitStopFacility> facilityId, double delay) {
		super(time);
		this.vehicleId = vehicleId;
		this.facilityId = facilityId;
		this.delay = delay;
	}

	/*
	 * Why is this getter deprecated? There is no obvious and simple alternative way to find the delay. The only
	 * alternative seems to be to try to indentify the transitLine, transitRoute and departure and its TransitRouteStop.
	 * That is cumbersome and not even a unique value (imagine the vehicle serves the same stop multiple times).
	 * vsp-gleich april'21
	 */
	@Deprecated
	public double getDelay() {
		return this.delay;
	}

	public Id<TransitStopFacility> getFacilityId() {
		return this.facilityId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public void setDelay(double delay) {
		this.delay = delay;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attributes.put(ATTRIBUTE_FACILITY, this.facilityId.toString());
		attributes.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		return attributes;
	}
}
