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

package org.matsim.contrib.transEnergySim.vehicles.api;

import java.util.LinkedHashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.MathLib;
import org.matsim.contrib.transEnergySim.agents.VehicleAgent;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlugType;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * Provides the basic structure for a vehicle running on battery.
 * 
 * @author User
 * 
 */
public abstract class VehicleWithBattery extends AbstractVehicle {

	/**
	 * often not the full capacity of a battery can be used or is recommended to
	 * be used, as this might reduce the life time of the battery.
	 */
	protected double usableBatteryCapacityInJoules;
	private boolean ignoreOverCharging = false;
	private static double overchargingErrorMargin=1;

	/**
	 * state of charge
	 */
	protected double socInJoules;
	protected double electricEnegyConsumedInJoules = 0.0, conventionalEnergyConsumedInJoules = 0.0;

	protected EnergyConsumptionModel electricDriveEnergyConsumptionModel;
	private EnergyConsumptionModel hybridDriveEnergyConsumptionModel;
	protected Id<Vehicle> vehicleId;
	private boolean isBEV = true; //TODO set this based on vehicle type
	private Double maxDischargingPowerInKW, maxLevel2ChargingPowerInKW, maxLevel3ChargingPowerInKW;
	private LinkedHashSet<ChargingPlugType> compatiblePlugTypes;
	private VehicleAgent agent;

	public double getRequiredEnergyInJoules() {
		double requiredEnergyInJoules = getUsableBatteryCapacityInJoules() - socInJoules;

		if (!MathLib.equals(requiredEnergyInJoules, 0, overchargingErrorMargin * 100) && requiredEnergyInJoules < 0) {
			DebugLib.stopSystemAndReportInconsistency("soc bigger than battery size");
		}

		return requiredEnergyInJoules;
	}

	public double getSocInJoules() {
		return socInJoules;
	}

	public void useBattery(double energyConsumptionInJoule) {
		socInJoules -= energyConsumptionInJoule;
		electricEnegyConsumedInJoules += energyConsumptionInJoule;
	}

	/**
	 * This method is operated by the charging scheme
	 * 
	 * @param energyChargeInJoule
	 * @return 
	 */
	public double addEnergyToVehicleBattery(double energyChargeInJoule) {
		if (!ignoreOverCharging) {
			socInJoules += energyChargeInJoule;
			if (!MathLib.equals(socInJoules, getUsableBatteryCapacityInJoules(), overchargingErrorMargin * 100) && socInJoules > getUsableBatteryCapacityInJoules()) {
				DebugLib.stopSystemAndReportInconsistency("the car has been overcharged soc(" + socInJoules + ") > battery capacity (" + getUsableBatteryCapacityInJoules() + ")");
			}
		}
		
		return energyChargeInJoule;
	}
	
	public double getUsableBatteryCapacityInJoules() {
		return usableBatteryCapacityInJoules;
	}

	@Override
	public void reset() {
		socInJoules = usableBatteryCapacityInJoules;
	}

	public void ignoreOverCharging(boolean ignoreOverCharging) {
		this.ignoreOverCharging = ignoreOverCharging;
	}

	@Override
	public Id<Vehicle> getId() {
		return this.vehicleId;
	}

	public double calcEndCharingTimeOfVehicle(double curTime, double chargerPowerInWatt){
		return curTime + getRequiredEnergyInJoules()/chargerPowerInWatt;
	}

	public double getMaxChargingPowerInKW(ChargingPlugType plugType) {
		switch(plugType.getNominalLevel()){
			case 1:
				return 1.5;
			case 2:
				return Math.min(getMaxLevel2ChargingPowerInKW(),plugType.getChargingPowerInKW());
			case 3:
				return Math.min(getMaxLevel3ChargingPowerInKW(),plugType.getChargingPowerInKW());
		}
		return 0.0;
	}
	public double getRemainingRangeInMiles() {
		return getRemainingRangeInMeters()/1609.34;
	}
	public double getRemainingRangeInMeters() {
		return this.socInJoules / this.electricDriveEnergyConsumptionModel.getEnergyConsumptionRateInJoulesPerMeter();
	}
	public boolean isBEV() {
		return this instanceof BatteryElectricVehicle;
	}
	public void setChargingFields(String vehicleTypeName, Double maxDischargingPowerInKW,
			Double maxLevel2ChargingPowerInKW, Double maxLevel3ChargingPowerInKW, LinkedHashSet<ChargingPlugType> compatiblePlugTypes) {
		this.maxDischargingPowerInKW = maxDischargingPowerInKW;
		this.maxLevel2ChargingPowerInKW = maxLevel2ChargingPowerInKW;
		this.maxLevel3ChargingPowerInKW = maxLevel3ChargingPowerInKW;
		this.compatiblePlugTypes = compatiblePlugTypes;
	}

	public Double getMaxDischargingPowerInKW() {
		return maxDischargingPowerInKW;
	}

	public void setMaxDischargingPowerInKW(Double maxDischargingPowerInKW) {
		this.maxDischargingPowerInKW = maxDischargingPowerInKW;
	}

	public Double getMaxLevel2ChargingPowerInKW() {
		return maxLevel2ChargingPowerInKW;
	}

	public Double getMaxLevel3ChargingPowerInKW() {
		return maxLevel3ChargingPowerInKW;
	}

	public LinkedHashSet<ChargingPlugType> getCompatiblePlugTypes() {
		return compatiblePlugTypes;
	}

	public boolean hasEnoughEnergyToDriveDistance(double nextLegTravelDistanceInMeters) {
		return getRemainingRangeInMeters() >= nextLegTravelDistanceInMeters;
	}

	public double getRequiredEnergyInJoulesToDriveDistance(double routeDistanceInMeters) {
		return routeDistanceInMeters * this.electricDriveEnergyConsumptionModel.getEnergyConsumptionRateInJoulesPerMeter();
	}
	
	public void setVehicleAgent(VehicleAgent agent){
		this.agent = agent;
	}
	public VehicleAgent getVehicleAgent(){
		return this.agent;
	}
	public EnergyConsumptionModel getElectricDriveEnergyConsumptionModel() {
		return electricDriveEnergyConsumptionModel;
	}

	public void setElectricDriveEnergyConsumptionModel(
			EnergyConsumptionModel electricDriveEnergyConsumptionModel) {
		this.electricDriveEnergyConsumptionModel = electricDriveEnergyConsumptionModel;
	}

	public EnergyConsumptionModel getHybridDriveEnergyConsumptionModel() {
		return hybridDriveEnergyConsumptionModel;
	}

	public void setHybridDriveEnergyConsumptionModel(
			EnergyConsumptionModel hybridDriveEnergyConsumptionModel) {
		this.hybridDriveEnergyConsumptionModel = hybridDriveEnergyConsumptionModel;
	}

	public void useHybridFuel(double hybridEnergyConsumed) {
		this.conventionalEnergyConsumedInJoules += hybridEnergyConsumed;
	}

}
