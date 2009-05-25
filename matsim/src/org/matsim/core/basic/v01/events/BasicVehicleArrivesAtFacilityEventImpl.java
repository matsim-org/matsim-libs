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

package org.matsim.core.basic.v01.events;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.events.BasicEventImpl;

/**
 * @author mrieser
 */
public class BasicVehicleArrivesAtFacilityEventImpl extends BasicEventImpl implements
		BasicVehicleArrivesAtFacilityEvent {

	private final Id vehicleId;
	private final Id facilityId;

	public BasicVehicleArrivesAtFacilityEventImpl(final double time, final Id vehicleId, final Id facilityId) {
		super(time);
		this.vehicleId = vehicleId;
		this.facilityId = facilityId;
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
		return attributes;
	}

	@Override
	public String getTextRepresentation() {
		throw new UnsupportedOperationException("writing to txt events is not supported.");
	}

}
