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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.MathLib;
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

	/**
	 * state of charge
	 */
	protected double socInJoules;

	protected EnergyConsumptionModel electricDriveEnergyConsumptionModel;
	protected Id<Vehicle> vehicleId;

	public double getRequiredEnergyInJoules() {
		double requiredEnergyInJoules = getUsableBatteryCapacityInJoules() - socInJoules;

		if (!MathLib.equals(requiredEnergyInJoules, 0, GeneralLib.EPSILON * 100) && requiredEnergyInJoules < 0) {
			DebugLib.stopSystemAndReportInconsistency("soc bigger than battery size");
		}

		return requiredEnergyInJoules;
	}

	public double getSocInJoules() {
		return socInJoules;
	}

	public void useBattery(double energyConsumptionInJoule) {
		socInJoules -= energyConsumptionInJoule;
	}

	/**
	 * This method is operated by the charging scheme
	 * 
	 * @param energyChargeInJoule
	 * @return 
	 */
	public double chargeVehicle(double energyChargeInJoule) {
		if (!ignoreOverCharging) {
			socInJoules += energyChargeInJoule;
			if (!MathLib.equals(socInJoules, getUsableBatteryCapacityInJoules(), GeneralLib.EPSILON * 100)
					&& socInJoules > getUsableBatteryCapacityInJoules()) {
				DebugLib.stopSystemAndReportInconsistency(
						"the car has been overcharged soc(" + socInJoules + ") > battery capacity (" + getUsableBatteryCapacityInJoules() + ")");
			}
		}
		
		return energyChargeInJoule;
	}
	
	public double chargeVehicle(double duration, double chargerPowerInWatt){
		return chargeVehicle(duration*chargerPowerInWatt);
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
	
}
