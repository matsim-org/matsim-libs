/* *********************************************************************** *
 * project: org.matsim.*
 * AgentStuckEvent.java
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
import org.matsim.vehicles.Vehicle;

public class VehicleAbortsEvent extends Event implements  HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "vehicle aborts";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;

	public VehicleAbortsEvent(final double time, final Id<Vehicle> vehicleId, final Id<Link> linkId) {
		super(time);
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		// linkId, vehicleId handled by superclass
		return atts;
	}
}
