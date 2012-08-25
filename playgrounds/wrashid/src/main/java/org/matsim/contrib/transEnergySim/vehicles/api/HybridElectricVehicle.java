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
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.api.EnergyConsumptionModel;

/**
 * vehicle has both combustion engine and battery on board
 * @author wrashid
 *
 */
public abstract class HybridElectricVehicle extends VehicleWithBattery {

	// TODO: both depletion mode and sustaining mode and other modes of use
	// e.g. one mode which just switches to hybrid mode (soc not changing), when battery empty
	// => in this case battery only chargable when connected to grid.
	// => perhaps, this is also the only mode, we want to support here, because the other mode
	// is just a normal hybrid, which d
	
	//TODO: irgendwo erwaehnen, dass wir annehmen, dass die network link length in meters angegeben sind!
	// else provide tool to update network.
	
	
	EnergyConsumptionModel combustionEngineECM;
	
	public void updateEnergyUse(Link link, double averageSpeedDriven){
		double energyConsumptionForLinkInJoule;
		if (socInJoules>0){
			energyConsumptionForLinkInJoule = electricDriveEnergyConsumptionModel.getEnergyConsumptionForLinkInJoule(link, averageSpeedDriven);
		
			if (energyConsumptionForLinkInJoule<=socInJoules){
				useBattery(energyConsumptionForLinkInJoule);
			} else {
				double fractionOfLinkTravelWithBattery=socInJoules/energyConsumptionForLinkInJoule;
				useBattery(socInJoules);
				
				energyConsumptionForLinkInJoule=combustionEngineECM.getEnergyConsumptionForLinkInJoule(link.getLength()*(1-fractionOfLinkTravelWithBattery), link.getFreespeed(), averageSpeedDriven);
				useCombustionEngine(energyConsumptionForLinkInJoule);
			}
		} else {
			energyConsumptionForLinkInJoule=combustionEngineECM.getEnergyConsumptionForLinkInJoule(link, averageSpeedDriven);
			useCombustionEngine(energyConsumptionForLinkInJoule);
		}
	}
	
	private void useCombustionEngine(double energyConsumptionInJoule){
		// TODO: log this...
	}
	
}
