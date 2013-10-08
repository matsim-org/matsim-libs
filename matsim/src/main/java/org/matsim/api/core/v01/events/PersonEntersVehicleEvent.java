/* *********************************************************************** *
 * project: org.matsim.*
 * PersonEntersVehicleEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.HasPersonId;

/**
 * @author mrieser
 */
public class PersonEntersVehicleEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "PersonEntersVehicle";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	private final Id vehicleId;
	private final Id personId;

	/*package*/ public PersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		super(time);
		this.personId = personId;
		this.vehicleId = vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_PERSON, this.personId.toString());
		attrs.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		return attrs;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id getVehicleId() {
		return this.vehicleId;
	}

	public Id getPersonId() {
		return this.personId;
	}
	
}
