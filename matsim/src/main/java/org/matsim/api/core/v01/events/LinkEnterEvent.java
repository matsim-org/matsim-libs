/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnterEvent.java
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

package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class LinkEnterEvent extends Event {

	public static final String EVENT_TYPE = "entered link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_PERSON = "person";
	
	private final Id<Person> driverId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	public LinkEnterEvent(final double time, final Id<Person> driverId, final Id<Link> linkId, Id<Vehicle> vehicleId) {
		super(time);
		this.driverId = driverId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	/**
	 * Please use getVehicleId() instead. 
	 * Vehicle-driver relations can be made by Wait2Link and VehicleLeavesTraffic Events.
	 */
	Id<Person> getDriverId() {
		return this.driverId;
	}	

	public Id<Link> getLinkId() {
		return this.linkId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.driverId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		return attr;
	}
}