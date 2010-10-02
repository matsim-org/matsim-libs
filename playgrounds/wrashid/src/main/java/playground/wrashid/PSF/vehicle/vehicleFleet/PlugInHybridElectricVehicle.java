/* *********************************************************************** *
 * project: org.matsim.*
 * PHEV.java
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

package playground.wrashid.PSF.vehicle.vehicleFleet;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.lib.DebugLib;

public class PlugInHybridElectricVehicle extends Vehicle {

	double batterySizeInJoule;
	double batteryMinThresholdInJoule;
	double currentBatteryChargeInJoule;
	
	double electricEnergyUseInJouleDuringDay;
	
	public PlugInHybridElectricVehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleId, Id vehicleClassId) {
		super(energyStateMaintainer, vehicleId, vehicleClassId);
	}

	@Override
	public void updateEnergyState(double energyConsumptionOnLinkInJoule) {
		logEnergyConsumption(energyConsumptionOnLinkInJoule);
		
		if (getAvailbleBatteryCharge()>=energyConsumptionOnLinkInJoule){
			processElectricityUsage(energyConsumptionOnLinkInJoule);
		} else if (getAvailbleBatteryCharge()>0){
			processElectricityUsage(getAvailbleBatteryCharge());
		}
	}
	
	private void processElectricityUsage(double energyConsumptionInJoule){
		electricEnergyUseInJouleDuringDay+=energyConsumptionInJoule;
		currentBatteryChargeInJoule-=energyConsumptionInJoule;
		
		if (currentBatteryChargeInJoule<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
	}
	
	private double getAvailbleBatteryCharge(){
		return currentBatteryChargeInJoule-batteryMinThresholdInJoule;
	}

	public double getRequiredBatteryCharge(){
		return batterySizeInJoule - currentBatteryChargeInJoule;
	}
	
}
