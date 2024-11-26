/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.core.utils.io.XmlUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class TransitDriverStartsEvent extends Event {

	public static final String EVENT_TYPE = "TransitDriverStarts";
	public static final String ATTRIBUTE_DRIVER_ID = "driverId";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_TRANSIT_LINE_ID = "transitLineId";
	public static final String ATTRIBUTE_TRANSIT_ROUTE_ID = "transitRouteId";
	public static final String ATTRIBUTE_DEPARTURE_ID = "departureId";
	private final Id<Person> driverId;
	private final Id<Vehicle> vehicleId;
	private final Id<TransitRoute> transitRouteId;
	private final Id<TransitLine> transitLineId;
	private final Id<Departure> departureId;

	public TransitDriverStartsEvent(final double time, final Id<Person> driverId, final Id<Vehicle> vehicleId,
			final Id<TransitLine> transitLineId, final Id<TransitRoute> transitRouteId, final Id<Departure> departureId) {
		super(time);
		this.driverId = driverId;
		this.vehicleId = vehicleId;
		this.transitRouteId = transitRouteId;
		this.transitLineId = transitLineId;
		this.departureId = departureId;
	}

	public Id<Person> getDriverId() {
		return driverId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<TransitRoute> getTransitRouteId() {
		return transitRouteId;
	}

	public Id<TransitLine> getTransitLineId() {
		return transitLineId;
	}

	public Id<Departure> getDepartureId() {
		return departureId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_DRIVER_ID, this.getDriverId().toString());
		atts.put(ATTRIBUTE_VEHICLE_ID, this.getVehicleId().toString());
		atts.put(ATTRIBUTE_TRANSIT_LINE_ID, this.getTransitLineId().toString());
		atts.put(ATTRIBUTE_TRANSIT_ROUTE_ID, this.getTransitRouteId().toString());
		atts.put(ATTRIBUTE_DEPARTURE_ID, this.getDepartureId().toString());
		return atts;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		writeXMLStart(out);

		XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_DRIVER_ID, this.getDriverId().toString());
		XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_VEHICLE_ID, this.getVehicleId().toString());
		XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_TRANSIT_LINE_ID, this.getTransitLineId().toString());
		XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_TRANSIT_ROUTE_ID, this.getTransitRouteId().toString());
		XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_DEPARTURE_ID, this.getDepartureId().toString());

		writeXMLEnd(out);
	}
}
