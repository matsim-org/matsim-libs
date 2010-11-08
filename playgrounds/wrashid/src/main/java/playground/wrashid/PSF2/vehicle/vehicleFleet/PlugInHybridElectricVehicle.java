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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class PlugInHybridElectricVehicle extends Vehicle {

	private double batterySizeInJoule;
	private double batteryMinThresholdInJoule;
	private double currentBatteryChargeInJoule;
	
	protected double electricEnergyUseInJouleDuringDayForDriving;
	
	public PlugInHybridElectricVehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleClassId) {
		super(energyStateMaintainer, vehicleClassId);
	}
	
	public PlugInHybridElectricVehicle(Id vehicleClassId) {
		super(vehicleClassId);
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
		setCurrentBatteryChargeInJoule(getCurrentBatteryChargeInJoule() - energyConsumptionInJoule);
		
		if (getCurrentBatteryChargeInJoule()<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
	}
	
	private double getAvailbleBatteryCharge(){
		return getCurrentBatteryChargeInJoule()-getBatteryMinThresholdInJoule();
	}

	public double getRequiredBatteryCharge(){
		return getBatterySizeInJoule() - getCurrentBatteryChargeInJoule();
	}

	
	private void chargeVehicle(double energyConsumptionInJoule){
		setCurrentBatteryChargeInJoule(getCurrentBatteryChargeInJoule() + energyConsumptionInJoule);
		
		if (getCurrentBatteryChargeInJoule()>getBatterySizeInJoule()){
			DebugLib.stopSystemAndReportInconsistency();
		}
	}
	
	public void centralizedCharging(double arrivalTime, double chargingDuration, double plugSizeInWatt, ActivityEndEvent event) {
		double chargeInJoule=chargingDuration*plugSizeInWatt;
		
		logChargingTime(arrivalTime, chargingDuration, chargeInJoule, event);
		
		chargeVehicle(chargeInJoule);
	}

	private void logChargingTime(double arrivalTime, double chargingDuration, double chargeInJoule, ActivityEndEvent event) {
		Id personId=ParametersPSF2.vehicles.getKey(this);
		double endChargingTime=GeneralLib.projectTimeWithin24Hours(arrivalTime+chargingDuration);
		if (event==null){
			System.out.println();
		}
		
		if (ParametersPSF2.chargingTimes.get(personId)==null){
			ParametersPSF2.chargingTimes.put(personId, new ChargingTimes());
		}
		
		ParametersPSF2.chargingTimes.get(personId).addChargeLog(new ChargeLog(event.getLinkId(), GeneralLib.projectTimeWithin24Hours(arrivalTime), endChargingTime, getCurrentBatteryChargeInJoule(), getCurrentBatteryChargeInJoule()+chargeInJoule, event.getFacilityId()));
	}

	public void setBatterySizeInJoule(double batterySizeInJoule) {
		this.batterySizeInJoule = batterySizeInJoule;
	}

	public double getBatterySizeInJoule() {
		return batterySizeInJoule;
	}

	public void setBatteryMinThresholdInJoule(double batteryMinThresholdInJoule) {
		this.batteryMinThresholdInJoule = batteryMinThresholdInJoule;
	}

	public double getBatteryMinThresholdInJoule() {
		return batteryMinThresholdInJoule;
	}

	public void setCurrentBatteryChargeInJoule(double currentBatteryChargeInJoule) {
		this.currentBatteryChargeInJoule = currentBatteryChargeInJoule;
	}

	public double getCurrentBatteryChargeInJoule() {
		return currentBatteryChargeInJoule;
	}
	
}
