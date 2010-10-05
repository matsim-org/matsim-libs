/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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
import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;

public abstract class Vehicle {

	private final EnergyStateMaintainer energyStateMaintainer;
	private Id vehicleId;
	private Id vehicleClassId;
	
	private double energyConcumptionForWholeDayInJoule;

	public Vehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleId, Id vehicleClassId){
		this.energyStateMaintainer = energyStateMaintainer;
		this.vehicleId=vehicleId;
		this.vehicleClassId=vehicleClassId;
	}
	
	public abstract void updateEnergyState(double energyConsumptionOnLinkInJoule);
	
	public void updateEnergyState(double timeSpendOnLink, Link link){
		energyStateMaintainer.processVehicleEnergyState(this, timeSpendOnLink, link);
	}

	public Id getVehicleId() {
		return vehicleId;
	}

	public Id getVehicleClassId() {
		return vehicleClassId;
	}
	
	public void logEnergyConsumption(double energyConsumptionInJoule){
		this.energyConcumptionForWholeDayInJoule+=energyConsumptionInJoule;
	}
	
	public void setEnergyConcumptionForWholeDayInJoule(double energyConcumptionForWholeDayInJoule) {
		this.energyConcumptionForWholeDayInJoule = energyConcumptionForWholeDayInJoule;
	}

	public double getEnergyConcumptionForWholeDayInJoule() {
		return energyConcumptionForWholeDayInJoule;
	}
	
	public void resetEnergyConsumptionSum(){
		energyConcumptionForWholeDayInJoule=0;
	}
	
	public static double getAverageSpeedOfVehicleOnLink(double timeSpentOnLink, Link link){
		return link.getLength()/timeSpentOnLink;
	}
}
