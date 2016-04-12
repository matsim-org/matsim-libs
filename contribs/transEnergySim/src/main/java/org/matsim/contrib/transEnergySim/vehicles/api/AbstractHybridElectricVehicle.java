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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * vehicle has both combustion engine and battery on board
 * @author wrashid
 *
 */
public abstract class AbstractHybridElectricVehicle extends VehicleWithBattery {

	// TODO: implement both serial and hybrid versions
	
	protected EnergyConsumptionModel engineECM;
	
	@Override
	public double updateEnergyUse(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven) {
		double energyConsumptionForLinkInJoule;
		if (socInJoules>0){
			energyConsumptionForLinkInJoule = electricDriveEnergyConsumptionModel.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters,maxSpeedOnLink,averageSpeedDriven);
		
			if (energyConsumptionForLinkInJoule<=socInJoules){
				useBattery(energyConsumptionForLinkInJoule);
			} else {
				double fractionOfLinkTravelWithBattery=socInJoules/energyConsumptionForLinkInJoule;
				useBattery(socInJoules);
				
				energyConsumptionForLinkInJoule=engineECM.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters*(1-fractionOfLinkTravelWithBattery), maxSpeedOnLink, averageSpeedDriven);
				logEngineEnergyConsumption(energyConsumptionForLinkInJoule);
			}
		} else {
			energyConsumptionForLinkInJoule = electricDriveEnergyConsumptionModel.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters,maxSpeedOnLink,averageSpeedDriven);
			logEngineEnergyConsumption(energyConsumptionForLinkInJoule);
		}
		
		return energyConsumptionForLinkInJoule;
	}
	
	abstract protected void logEngineEnergyConsumption(double energyConsumptionInJoule);
	
}
