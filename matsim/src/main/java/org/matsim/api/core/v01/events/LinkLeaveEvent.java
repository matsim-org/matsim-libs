/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

public class LinkLeaveEvent extends Event implements HasLinkId {

	public static final String EVENT_TYPE = "left link";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final Id<Person> driverId;

	final static String missingDriverIdMessage = "driver (or person) ID does no longer exist in LinkEnter/LeaveEvent; use vehicle ID instead. "
			+ "See Vehicle2DriverEventHandler for an approach to reconstruct the driver ID and/or EventsConverterXML to convert your old event file.";

	public LinkLeaveEvent(final double time, final Id<Vehicle> vehicleId, final Id<Link> linkId, final Id<Person> driverId) {
		super(time);
		this.linkId = linkId;
		if ( vehicleId==null ) {
			throw new RuntimeException( LinkEnterEvent.missingVehicleIdMessage ) ;
		}
		this.vehicleId = vehicleId;
		this.driverId = driverId;
	}

	public LinkLeaveEvent(final double time, final Id<Vehicle> vehicleId, final Id<Link> linkId) {
		this(time, vehicleId,linkId, null);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Person> getDriverId() {
		return this.driverId;
	}

	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		return attr;
	}
}
