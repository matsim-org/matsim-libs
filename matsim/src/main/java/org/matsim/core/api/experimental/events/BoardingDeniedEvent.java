/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.vehicles.Vehicle;


/**
 * @author nagel
 *
 */
public final class BoardingDeniedEvent extends Event implements HasPersonId
{
	public static final String EVENT_TYPE="BoardingDeniedEvent";

	public static final String ATTRIBUTE_PERSON_ID = "person";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

	private Id<Person> personId;
	private Id<Vehicle> vehicleId;

	public BoardingDeniedEvent(final double time, Id<Person> personId, Id<Vehicle> vehicleId) {
		super(time);
		this.personId = personId;
		this.vehicleId = vehicleId;
	}

	public Id<Person> getPersonId() {
		return personId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String,String> getAttributes() {
		Map<String,String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_PERSON_ID, this.personId.toString());
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		return atts;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
