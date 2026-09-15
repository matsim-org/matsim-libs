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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Objects;

import static org.matsim.core.utils.io.XmlUtils.writeEncodedAttributeKeyValue;

/**
 * Event that indicates that a person continues the leg in a vehicle with a different Id, but without leaving the vehicle.
 *
 * @author rakow
 */
public class PersonContinuesInVehicleEvent extends Event implements HasPersonId, HasVehicleId {

	public static final String EVENT_TYPE = "PersonContinuesInVehicle";
	public static final String ATTRIBUTE_FROM_VEHICLE = "fromVehicle";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String ATTRIBUTE_TRANSIT_LINE = "transitLine";
	public static final String ATTRIBUTE_TRANSIT_ROUTE = "transitRoute";

	private final Id<Person> personId;
	private final Id<Vehicle> fromVehicleId;
	private final Id<Vehicle> vehicleId;
	private final Id<TransitStopFacility> stopFacilityId;
	private final Id<TransitLine> transitLineId;
	private final Id<TransitRoute> transitRouteId;


	public PersonContinuesInVehicleEvent(final double time, final Id<Person> personId,
										 final Id<Vehicle> fromVehicleId, final Id<Vehicle> vehicleId, Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
		this(time, personId, fromVehicleId, vehicleId, null, transitLineId, transitRouteId);
	}

	public PersonContinuesInVehicleEvent(final double time, final Id<Person> personId,
										 final Id<Vehicle> fromVehicleId, final Id<Vehicle> vehicleId,
										 final Id<TransitStopFacility> stopFacilityId, Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
		super(time);
		this.personId = personId;
		this.fromVehicleId = fromVehicleId;
		this.vehicleId = vehicleId;
		this.stopFacilityId = stopFacilityId;
		this.transitLineId = transitLineId;
		this.transitRouteId = transitRouteId;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public Id<TransitStopFacility> getStopFacilityId() {
		return stopFacilityId;
	}

	public Id<TransitLine> getTransitLineId() {
		return transitLineId;
	}

	public Id<TransitRoute> getTransitRouteId() {
		return transitRouteId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();

		// personId, vehicleId handled by superclass
		if (this.stopFacilityId != null)
			attrs.put(ATTRIBUTE_FACILITY, this.stopFacilityId.toString());

		if (fromVehicleId != null)
			attrs.put(ATTRIBUTE_FROM_VEHICLE, Objects.toString(fromVehicleId));

		attrs.put(ATTRIBUTE_TRANSIT_LINE, transitLineId.toString());
		attrs.put(ATTRIBUTE_TRANSIT_ROUTE, transitRouteId.toString());

		return attrs;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes all common attributes
		writeXMLStart(out);

		writeEncodedAttributeKeyValue(out, ATTRIBUTE_FACILITY, Objects.toString(stopFacilityId, null));
		writeEncodedAttributeKeyValue(out, ATTRIBUTE_FROM_VEHICLE, Objects.toString(fromVehicleId, null));
		writeEncodedAttributeKeyValue(out, ATTRIBUTE_TRANSIT_LINE, Objects.toString(transitLineId, null));
		writeEncodedAttributeKeyValue(out, ATTRIBUTE_TRANSIT_ROUTE, Objects.toString(transitRouteId, null));

		writeXMLEnd(out);
	}
}
