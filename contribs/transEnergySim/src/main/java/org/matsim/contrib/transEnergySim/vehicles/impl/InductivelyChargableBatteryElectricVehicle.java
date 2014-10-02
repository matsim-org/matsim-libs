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
import org.matsim.contrib.transEnergySim.vehicles.api.InductivlyChargable;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * Inductively chargeable, battery electric vehicle
 * @author wrashid
 *
 */
public class InductivelyChargableBatteryElectricVehicle extends BatteryElectricVehicle implements InductivlyChargable {

	public InductivelyChargableBatteryElectricVehicle(EnergyConsumptionModel ecm, double batteryCapacityInJoules){
		this.electricDriveEnergyConsumptionModel=ecm;
		this.usableBatteryCapacityInJoules=batteryCapacityInJoules;
		this.socInJoules=batteryCapacityInJoules;
	}

 
	 
}
