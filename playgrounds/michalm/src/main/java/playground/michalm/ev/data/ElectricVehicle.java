/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.ev.data;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.discharging.*;

public interface ElectricVehicle extends Identifiable<Vehicle> {
	DriveEnergyConsumption getDriveEnergyConsumption();

	AuxEnergyConsumption getAuxEnergyConsumption();

	Battery getBattery();

	Battery swapBattery(Battery battery);
}
