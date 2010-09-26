/* *********************************************************************** *
 * project: org.matsim.*
 * FossilFuelConsumingVehicle.java
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

package playground.wrashid.PSF.vehicle;

import org.matsim.api.core.v01.Id;

/**
 * Used to model: gasoline, diesel and other vehicle types only using one energy
 * source. The assumption is, that the model does not consider the tank size of
 * these vehicles.
 * 
 * @author wrashid
 * 
 */
public class SingleEnergySourceVehicleWithInfinitPowerTank extends Vehicle {

	private double energyConcumptionForWholeDayInJoule;
	
	public SingleEnergySourceVehicleWithInfinitPowerTank(EnergyStateMaintainer energyStateMaintainer, Id vehicleId, Id vehicleClassId) {
		super(energyStateMaintainer, vehicleClassId, vehicleClassId);
	}

	public void addEnergyConcumption(double energyConsumptionInJoule){
		this.energyConcumptionForWholeDayInJoule+=energyConsumptionInJoule;
	}
	
	public void setEnergyConcumptionForWholeDayInJoule(double energyConcumptionForWholeDayInJoule) {
		this.energyConcumptionForWholeDayInJoule = energyConcumptionForWholeDayInJoule;
	}

	public double getEnergyConcumptionForWholeDayInJoule() {
		return energyConcumptionForWholeDayInJoule;
	}

}
