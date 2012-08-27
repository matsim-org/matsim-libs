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

import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.api.EnergyConsumptionModel;

import playground.wrashid.lib.DebugLib;

public abstract class VehicleWithBattery implements Vehicle {

	/**
	 * often not the full capacity of a battery can be used or is recommended to
	 * be used, as this might reduce the life time of the battery.
	 */
	protected double usableBatteryCapacityInJoules;
	
	/**
	 * state of charge
	 */
	protected double socInJoules;
	
	protected EnergyConsumptionModel electricDriveEnergyConsumptionModel;

	public double getRequiredEnergyInJoules(){
		double requiredEnergyInJoules = getUsableBatteryCapacityInJoules()-socInJoules;
		
		if (requiredEnergyInJoules<0){
			DebugLib.stopSystemAndReportInconsistency("soc bigger than battery size");
		}
		
		return requiredEnergyInJoules;
	}
	
	public double getSocInJoules(){
		return socInJoules;
	}
	
	public void useBattery(double energyConsumptionInJoule){
		socInJoules-=energyConsumptionInJoule;
	}
	
	/**
	 * This method is operated by the charging scheme
	 * @param energyChargeInJoule
	 */
	public void chargeBattery(double energyChargeInJoule){
		socInJoules+=energyChargeInJoule;

		if (socInJoules>getUsableBatteryCapacityInJoules()){
			DebugLib.stopSystemAndReportInconsistency("the car has been overcharged");
		}
	}

	public double getUsableBatteryCapacityInJoules() {
		return usableBatteryCapacityInJoules;
	}

}
