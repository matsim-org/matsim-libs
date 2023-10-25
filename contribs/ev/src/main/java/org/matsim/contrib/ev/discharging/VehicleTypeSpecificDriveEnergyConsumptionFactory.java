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
/*
 * created by jbischoff, 11.10.2018
 */

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.vehicles.VehicleType;

public final class VehicleTypeSpecificDriveEnergyConsumptionFactory implements DriveEnergyConsumption.Factory {

	private final Map<Id<VehicleType>, DriveEnergyConsumption.Factory> consumptionMap = new HashMap<>();

	public void addEnergyConsumptionModelFactory(Id<VehicleType> vehicleTypeId,
			DriveEnergyConsumption.Factory driveEnergyConsumption) {
		consumptionMap.put(vehicleTypeId, driveEnergyConsumption);
	}

	@Override
	public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
		var vehicleType = electricVehicle.getVehicleSpecification().getMatsimVehicle().getType().getId();
		DriveEnergyConsumption c = consumptionMap.get(vehicleType).create(electricVehicle);
		if (c == null) {
			throw new RuntimeException(
					"No EnergyConsumptionModel for VehicleType " + vehicleType + " has been defined.");
		}
		return c;
	}
}
