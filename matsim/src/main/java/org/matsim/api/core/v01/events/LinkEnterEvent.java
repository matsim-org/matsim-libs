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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class LinkEnterEvent extends Event implements HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "entered link";

	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	final static String missingVehicleIdMessage = "vehicleId=null in LinkEnter/LeaveEvent; this would cause problems downstream thus we are not accepting it";

	public LinkEnterEvent(final double time, final Id<Vehicle> vehicleId, final Id<Link> linkId) {
		super(time);
		this.linkId = linkId;
		if ( vehicleId==null ) {
			throw new RuntimeException( missingVehicleIdMessage ) ;
		}
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	/**
	 * Please use getVehicleId() instead.
	 * Vehicle-driver relations can be made by {@link VehicleEntersTrafficEvent} and {@link VehicleLeavesTrafficEvent}.
	 */
	@Deprecated
	public Id<Person> getDriverId() {
		throw new RuntimeException( LinkLeaveEvent.missingDriverIdMessage ) ;
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
		Map<String, String> atts = super.getAttributes();
		// linkId, vehicleId handled by superclass
		return atts;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes all common attributes
		writeXMLStart(out);
		writeXMLEnd(out);
	}
}
