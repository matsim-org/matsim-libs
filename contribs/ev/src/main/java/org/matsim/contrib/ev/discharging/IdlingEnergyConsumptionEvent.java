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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class IdlingEnergyConsumptionEvent extends AbstractEnergyConsumptionEvent {
	public static final String EVENT_TYPE = "idlingEnergyConsumption";

	public IdlingEnergyConsumptionEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, double energy, double endCharge) {
		super(time, vehicleId, linkId, energy, endCharge);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static IdlingEnergyConsumptionEvent convert(GenericEvent genericEvent) {
		Map<String, String> attributes = genericEvent.getAttributes();
		double time = genericEvent.getTime();
		Id<Vehicle> vehicleId = Id.createVehicleId(attributes.get(IdlingEnergyConsumptionEvent.ATTRIBUTE_VEHICLE));
		Id<Link> linkId = Id.createLinkId(attributes.get(IdlingEnergyConsumptionEvent.ATTRIBUTE_LINK));
		double energy = Double.parseDouble(attributes.get(IdlingEnergyConsumptionEvent.ATTRIBUTE_ENERGY));
		double endCharge = Double.parseDouble(attributes.get(IdlingEnergyConsumptionEvent.ATTRIBUTE_END_CHARGE));
		return new IdlingEnergyConsumptionEvent(time, vehicleId, linkId, energy, endCharge);
	}
}
