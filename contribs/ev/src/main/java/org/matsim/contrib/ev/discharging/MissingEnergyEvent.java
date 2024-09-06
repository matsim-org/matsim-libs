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

package org.matsim.contrib.ev.discharging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public final class MissingEnergyEvent extends Event {
	public static final String EVENT_TYPE = "missing_energy";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_ENERGY = "energy";
	public static final String ATTRIBUTE_LINK = "link";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;
	private final double energy;

	public MissingEnergyEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, double energy) {
		super(time);
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.energy = energy;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public double getEnergy() {
		return energy;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, vehicleId + "");
		attr.put(ATTRIBUTE_LINK, linkId + "");
		attr.put(ATTRIBUTE_ENERGY, energy + "");
		return attr;
	}
}
