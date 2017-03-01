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

package playground.michalm.ev.charging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.data.Charger;

public class ChargingStartEvent extends Event {
	public static final String EVENT_TYPE = "charging_start";
	public static final String ATTRIBUTE_CHARGER = "charger";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	private final Id<Charger> chargerId;
	private final Id<Vehicle> vehicleId;

	public ChargingStartEvent(double time, Id<Charger> chargerId, Id<Vehicle> vehicleId) {
		super(time);
		this.chargerId = chargerId;
		this.vehicleId = vehicleId;
	}

	public Id<Charger> getChargerId() {
		return chargerId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_CHARGER, chargerId.toString());
		attr.put(ATTRIBUTE_VEHICLE, vehicleId.toString());
		return attr;
	}
}
