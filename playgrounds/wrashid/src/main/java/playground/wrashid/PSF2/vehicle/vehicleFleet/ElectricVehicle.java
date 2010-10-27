/* *********************************************************************** *
 * project: org.matsim.*
 * EV.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.vehicle.vehicleFleet;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;


public class ElectricVehicle extends PlugInHybridElectricVehicle {

	public ElectricVehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleClassId) {
		super(energyStateMaintainer, vehicleClassId);
	}

	public double getBatteryOverForWholeDayUsageInJoules(){
		return energyConcumptionForWholeDayInJoule-electricEnergyUseInJouleDuringDayForDriving;
	}
	
	
}
