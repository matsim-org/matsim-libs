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

package playground.wrashid.PSF2.vehicle.vehicleFleet;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.lib.DebugLib;

public class PlugInHybridElectricVehicle extends Vehicle {

	double batterySizeInJoule;
	double batteryMinThresholdInJoule;
	double currentBatteryChargeInJoule;
	
	double electricEnergyUseInJouleDuringDayForDriving;
	
	public PlugInHybridElectricVehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleClassId) {
		super(energyStateMaintainer, vehicleClassId);
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
		electricEnergyUseInJouleDuringDayForDriving+=energyConsumptionInJoule;
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

	
	private void chargeVehicle(double energyConsumptionInJoule){
		currentBatteryChargeInJoule+=energyConsumptionInJoule;
		
		if (currentBatteryChargeInJoule>batterySizeInJoule){
			DebugLib.stopSystemAndReportInconsistency();
		}
	}
	
	public void centralizedCharging(double arrivalTime, double chargingDuration, double plugSizeInWatt) {
		double chargeInJoule=chargingDuration*plugSizeInWatt;
		
		chargeVehicle(chargeInJoule);
		
		
		// write out the charging pattern
			// we need a handle to the charge output file here...
		
		
		// if needed, also log graph...
	}
	
}
