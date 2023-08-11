/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * @author Ricardo Ewert
 */

public class SelectNewParkingLocationEvent extends Event {
	public static final String EVENT_TYPE = "select new parking location";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_Current_LINK = "link";
	public static final String ATTRIBUTE_ParkingDestination_LINK = "parkingDestinationLink";
	private final Id<Link> currentLinkId;
	private final Id<Link> parkingDestinationLinkId;
	private final Id<Vehicle> vehicleId;

	public SelectNewParkingLocationEvent(final double time, Id<Vehicle> vehicleId, Id<Link> currentLinkId, Id<Link> parkingDestinationLinkId) {
		super(time);
		this.currentLinkId = currentLinkId;
		this.vehicleId = vehicleId;
		this.parkingDestinationLinkId = parkingDestinationLinkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	public Id<Link> getparkingDestinationLinkId() {
		return parkingDestinationLinkId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_Current_LINK, this.currentLinkId.toString());
		attr.put(ATTRIBUTE_ParkingDestination_LINK, this.parkingDestinationLinkId.toString());
		return attr;
	}

}
