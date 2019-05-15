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

package org.matsim.contrib.ev.discharging;/*
 * created by jbischoff, 11.10.2018
 */

import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.util.HashMap;
import java.util.Map;

public class VehicleTypeSpecificDriveEnergyConsumptionFactory implements DriveEnergyConsumption.Factory {

    private Map<String, DriveEnergyConsumption.Factory> consumptionMap = new HashMap<>();

    public void addEnergyConsumptionModelFactory(String vehicleType, DriveEnergyConsumption.Factory driveEnergyConsumption) {
		consumptionMap.put(vehicleType, driveEnergyConsumption);
	}

	@Override
	public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
        DriveEnergyConsumption c = consumptionMap.get(electricVehicle.getVehicleType()).create(electricVehicle);
		if (c == null) {
			throw new RuntimeException("No EnergyconsumptionModel for VehicleType "
					+ electricVehicle.getVehicleType()
					+ " has been defined.");
		}
		return c;
	}
}
