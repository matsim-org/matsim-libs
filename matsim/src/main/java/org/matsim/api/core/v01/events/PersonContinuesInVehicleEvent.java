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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Objects;

import static org.matsim.core.utils.io.XmlUtils.writeEncodedAttributeKeyValue;

/**
 * Event that indicates that a person continues the leg in a vehicle with a different Id, but without leaving the vehicle.
 * @author rakow
 */
public class PersonContinuesInVehicleEvent extends Event implements HasPersonId, HasVehicleId {

	public static final String EVENT_TYPE = "PersonContinuesInVehicle";
	public static final String ATTRIBUTE_FROM_VEHICLE = "fromVehicle";

	private final Id<Person> personId;
	private final Id<Vehicle> fromVehicleId;
	private final Id<Vehicle> vehicleId;

	/*package*/ public PersonContinuesInVehicleEvent(final double time, final Id<Person> personId,
													 final Id<Vehicle> fromVehicleId, final Id<Vehicle> vehicleId) {
		super(time);
		this.personId = personId;
		this.fromVehicleId = fromVehicleId;
		this.vehicleId = vehicleId;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();

		// personId, vehicleId handled by superclass
		attrs.put(ATTRIBUTE_FROM_VEHICLE, Objects.toString(fromVehicleId));

		return attrs;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes all common attributes
		writeXMLStart(out);

		writeEncodedAttributeKeyValue(out, ATTRIBUTE_FROM_VEHICLE, Objects.toString(fromVehicleId, null));

		writeXMLEnd(out);
	}
}
