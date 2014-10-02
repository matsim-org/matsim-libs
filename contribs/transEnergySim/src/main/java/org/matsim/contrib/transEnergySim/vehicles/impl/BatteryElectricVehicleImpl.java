/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.vehicles.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

public class BatteryElectricVehicleImpl extends BatteryElectricVehicle {
	/**
	 * 
	 * a fairly basic implementation of a BEV.
	 * 
	 * @param EnergyConsumptionModel
	 * @param battery capacity (in J) - this is assumed to be the SoC at iteration start
	 * 
	 * 
	 */
	public BatteryElectricVehicleImpl(EnergyConsumptionModel ecm, double usableBatteryCapacityInJoules, Id<Vehicle> vehicleId) {
		this.electricDriveEnergyConsumptionModel=ecm;
		this.usableBatteryCapacityInJoules=usableBatteryCapacityInJoules;
		this.socInJoules=usableBatteryCapacityInJoules;
		this.vehicleId = vehicleId;
	
	}

}
