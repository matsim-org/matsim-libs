/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public final class MotorizedInteractionEvent extends Event implements HasLinkId, HasVehicleId{
	// (plausible to have this public)

	private final Id<Link> linkId;
	private final Id<Vehicle> vehId;
	public MotorizedInteractionEvent(double time, Id<Link> linkId, Id<Vehicle> vehId) {
		super(time);
		this.linkId = linkId;
		this.vehId = vehId;
	}

	@Override public Id<Link> getLinkId() {
		return linkId;
	}

	@Override public Id<Vehicle> getVehicleId() {
		return vehId;
	}

	@Override public String getEventType() {
		return "motorizedInteraction";
	}
}
