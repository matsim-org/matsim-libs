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

package org.matsim.contrib.ev.charging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

public class ChargingStartEvent extends Event {
	public static final String EVENT_TYPE = "charging_start";
	public static final String ATTRIBUTE_CHARGER = "charger";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_TYPE = "chargerType";

	private final Id<Charger> chargerId;
	private final Id<ElectricVehicle> vehicleId;
	private final String chargerType;

	public ChargingStartEvent(double time, Id<Charger> chargerId, Id<ElectricVehicle> vehicleId, String chargerType) {
		super(time);
		this.chargerId = chargerId;
		this.vehicleId = vehicleId;
		this.chargerType = chargerType;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Charger> getChargerId() {
		return chargerId;
	}

	public Id<ElectricVehicle> getVehicleId() {
		return vehicleId;
	}

	public String getChargerType() {
		return chargerType;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_CHARGER, chargerId.toString());
		attr.put(ATTRIBUTE_VEHICLE, vehicleId.toString());
		attr.put(ATTRIBUTE_TYPE, chargerType);
		return attr;
	}
}
