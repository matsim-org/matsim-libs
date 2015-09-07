/* *********************************************************************** *
 * project: org.matsim.*
 * AgentWait2LinkEvent.java
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
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.vehicles.Vehicle;

public class Link2WaitEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "link2wait";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_NETWORKMODE = "networkMode";
	public static final String ATTRIBUTE_DRIVER = "person";

	private final Id<Person> driverId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final String networkMode;

	public Link2WaitEvent(final double time, final Id<Person> driverId, final Id<Link> linkId, Id<Vehicle> vehicleId, String networkMode) {
		super(time);
		this.driverId = driverId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.networkMode = networkMode;
	}
	
	public Id<Person> getPersonId() {
		return this.driverId;
	}	
	
	public Id<Link> getLinkId() {
		return this.linkId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}
	
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public String getNetworkMode() {
		return networkMode;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_DRIVER, this.driverId.toString());
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		if (this.networkMode != null) {
			attr.put(ATTRIBUTE_NETWORKMODE, networkMode);
		}
		return attr;
	}
}