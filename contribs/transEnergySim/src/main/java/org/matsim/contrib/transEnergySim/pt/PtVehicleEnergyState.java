/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.transEnergySim.pt;

import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

public class PtVehicleEnergyState {

	private EnergyConsumptionModel energyConsumptionModelElectric;
	private EnergyConsumptionModel energyConsumptionModelChemical;
	private double usableBatterySize;
	private double socInJoules;

	public PtVehicleEnergyState(double usableBatterySize,EnergyConsumptionModel energyConsumptionModelElectric, EnergyConsumptionModel energyConsumptionModelChemical){
		this.setUsableBatterySize(usableBatterySize);
		this.energyConsumptionModelElectric = energyConsumptionModelElectric;
		this.energyConsumptionModelChemical = energyConsumptionModelChemical;
		fullyChargeBattery();
	}

	public void fullyChargeBattery() {
		socInJoules=getUsableBatterySize();
	}
	
	public double useBattery(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven) {
		double energyConsumptionInJoule = energyConsumptionModelElectric.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters,maxSpeedOnLink,averageSpeedDriven);
		setSocInJoules(getSocInJoules() - energyConsumptionInJoule);
		return energyConsumptionInJoule;
	}
	
	public double useChemicalEnergy(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven) {
		return energyConsumptionModelChemical.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters,maxSpeedOnLink,averageSpeedDriven);
	}

	public double getSocInJoules() {
		return socInJoules;
	}

	private void setSocInJoules(double socInJoules) {
		this.socInJoules = socInJoules;
	}
	
	public void chargeVehicle(double energyInJoule){
		socInJoules+=energyInJoule;
	}

	public double getUsableBatterySize() {
		return usableBatterySize;
	}

	private void setUsableBatterySize(double usableBatterySize) {
		this.usableBatterySize = usableBatterySize;
	}
	
}

