/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.discharging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractEnergyConsumptionEvent extends Event implements HasLinkId, HasVehicleId {
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_ENERGY = "energy";
	public static final String ATTRIBUTE_END_CHARGE = "endCharge";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;
	private final double energy;
	private final double endCharge;

	public AbstractEnergyConsumptionEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, double energy, double endCharge) {
		super(time);
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.energy = energy;
		this.endCharge = endCharge;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public double getEnergy() {
		return energy;
	}

	public double getEndCharge() {
		return endCharge;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, vehicleId + "");
		attr.put(ATTRIBUTE_LINK, linkId + "");
		attr.put(ATTRIBUTE_ENERGY, energy + "");
		attr.put(ATTRIBUTE_END_CHARGE, endCharge + "");
		return attr;
	}
}
