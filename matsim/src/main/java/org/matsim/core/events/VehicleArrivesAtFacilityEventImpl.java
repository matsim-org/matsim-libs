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

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser
 */
public class VehicleArrivesAtFacilityEventImpl extends EventImpl implements
		VehicleArrivesAtFacilityEvent {

	private final Id vehicleId;
	private final Id facilityId;
	private final double delay;

	public VehicleArrivesAtFacilityEventImpl(final double time, final Id vehicleId, final Id facilityId, double delay) {
		super(time);
		this.vehicleId = vehicleId;
		this.facilityId = facilityId;
		this.delay = delay;
	}
	
	public double getDelay() {
		return this.delay;
	}

	public Id getFacilityId() {
		return this.facilityId;
	}

	public Id getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
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
