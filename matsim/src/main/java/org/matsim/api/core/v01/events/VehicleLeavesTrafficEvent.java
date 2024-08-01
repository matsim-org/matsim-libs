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
import org.matsim.vehicles.Vehicle;

public class VehicleLeavesTrafficEvent extends Event implements HasPersonId, HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "vehicle leaves traffic";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_NETWORKMODE = "networkMode";
	public static final String ATTRIBUTE_DRIVER = "person";
	public static final String ATTRIBUTE_POSITION = "relativePosition";

	private final Id<Person> driverId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final String networkMode;
	private final double relativePositionOnLink;


	public VehicleLeavesTrafficEvent(final double time, final Id<Person> driverId, final Id<Link> linkId, Id<Vehicle> vehicleId, String networkMode, double relativePositionOnLink) {
		super(time);
		this.driverId = driverId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.networkMode = networkMode;
		this.relativePositionOnLink = relativePositionOnLink;

	}

	@Override
	public Id<Person> getPersonId() {
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
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getNetworkMode() {
		return networkMode;
	}

	public double getRelativePositionOnLink() {
		return relativePositionOnLink;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// personId, linkId, vehicleId handled by superclass
		if (this.networkMode != null) {
			attr.put(ATTRIBUTE_NETWORKMODE, networkMode);
		}
		attr.put(ATTRIBUTE_POSITION, Double.toString(this.relativePositionOnLink));
		return attr;
	}
}
