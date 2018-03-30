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

package org.matsim.vsp.ev.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class EvDataImpl implements EvData {
	private final Map<Id<ElectricVehicle>, ElectricVehicle> eVehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<ElectricVehicle>, ElectricVehicle> getElectricVehicles() {
		return Collections.unmodifiableMap(eVehicles);
	}

	@Override
	public void addElectricVehicle(Id<ElectricVehicle> vehicleId, ElectricVehicle ev) {
		eVehicles.put(vehicleId, ev);
	}

	@Override
	public void resetBatteries() {
		for (ElectricVehicle ev : eVehicles.values()) {
			ev.getBattery().resetSoc();
		}
	}
}
