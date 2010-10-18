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

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser
 */
public class TransitDriverStartsEvent extends EventImpl {

	public static final String EVENT_TYPE = "TransitDriverStarts";
	public static final String ATTRIBUTE_DRIVER_ID = "driverId";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_TRANSIT_LINE_ID = "transitLineId";
	public static final String ATTRIBUTE_TRANSIT_ROUTE_ID = "transitRouteId";
	public static final String ATTRIBUTE_DEPARTURE_ID = "departureId";
	private final Id driverId;
	private final Id vehicleId;
	private final Id transitRouteId;
	private final Id transitLineId;
	private final Id departureId;

	public TransitDriverStartsEvent(final double time, final Id driverId, final Id vehicleId, final Id transitLineId, final Id transitRouteId, final Id departureId) {
		super(time);
		this.driverId = driverId;
		this.vehicleId = vehicleId;
		this.transitRouteId = transitRouteId;
		this.transitLineId = transitLineId;
		this.departureId = departureId;
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
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id getDriverId() {
		return driverId;
	}

	public Id getVehicleId() {
		return vehicleId;
	}

	public Id getTransitRouteId() {
		return transitRouteId;
	}

	public Id getTransitLineId() {
		return transitLineId;
	}

	public Id getDepartureId() {
		return departureId;
	}

}
