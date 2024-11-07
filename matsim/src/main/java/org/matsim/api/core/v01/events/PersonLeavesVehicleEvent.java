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
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author mrieser
 */
public class PersonLeavesVehicleEvent extends Event implements HasPersonId, HasVehicleId {

	public static final String EVENT_TYPE = "PersonLeavesVehicle";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	private final Id<Person> personId;
	private Id<Vehicle> vehicleId;

	/*package*/ public PersonLeavesVehicleEvent(final double time, final Id<Person> personId, final Id<Vehicle> vehicleId) {
		super(time);
		this.personId = personId;
		this.vehicleId = vehicleId;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		// personId, vehicleId handled by superclass
		return attrs;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes all common attributes
		writeXMLStart(out);
		writeXMLEnd(out);
	}
}
