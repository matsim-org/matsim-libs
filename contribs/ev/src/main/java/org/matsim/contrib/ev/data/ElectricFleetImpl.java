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

package org.matsim.contrib.ev.data;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElectricFleetImpl implements ElectricFleet {
	private final Map<Id<ElectricVehicle>, ElectricVehicle> eVehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<ElectricVehicle>, ElectricVehicle> getElectricVehicles() {
		return Collections.unmodifiableMap(eVehicles);
	}

	public void addElectricVehicle(ElectricVehicle ev) {
		eVehicles.put(ev.getId(), ev);
	}

	@Override
	public void resetBatteriesAndConsumptions(DriveEnergyConsumption.Factory driveConsumptionFactory,
			AuxEnergyConsumption.Factory auxConsumptionFactory) {
		for (ElectricVehicle ev : eVehicles.values()) {
			ev.getBattery().resetSoc();
			ev.setDriveEnergyConsumption(driveConsumptionFactory.create(ev));
			if (auxConsumptionFactory != null) {
				ev.setAuxEnergyConsumption(auxConsumptionFactory.create(ev));
			}
		}
	}
}
