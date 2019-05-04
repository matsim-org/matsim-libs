/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.fleet;

import java.util.List;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

public interface ElectricVehicle extends Identifiable<ElectricVehicle> {
	DriveEnergyConsumption getDriveEnergyConsumption();

	AuxEnergyConsumption getAuxEnergyConsumption();

	Battery getBattery();

	List<String> getChargerTypes();

	String getVehicleType();

	void setDriveEnergyConsumption(DriveEnergyConsumption driveEnergyConsumption);

	void setAuxEnergyConsumption(AuxEnergyConsumption auxEnergyConsumption);
}
