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
public abstract class AbstractHybridElectricVehicle extends AbstractVehicleWithBattery {

	// TODO: implement both serial and hybrid versions

	
	
	EnergyConsumptionModel engineECM;
	
	public double updateEnergyUse(Link link, double averageSpeedDriven){
		double energyConsumptionForLinkInJoule;
		if (socInJoules>0){
			energyConsumptionForLinkInJoule = electricDriveEnergyConsumptionModel.getEnergyConsumptionForLinkInJoule(link, averageSpeedDriven);
		
			if (energyConsumptionForLinkInJoule<=socInJoules){
				useBattery(energyConsumptionForLinkInJoule);
			} else {
				double fractionOfLinkTravelWithBattery=socInJoules/energyConsumptionForLinkInJoule;
				useBattery(socInJoules);
				
				energyConsumptionForLinkInJoule=engineECM.getEnergyConsumptionForLinkInJoule(link.getLength()*(1-fractionOfLinkTravelWithBattery), link.getFreespeed(), averageSpeedDriven);
				useCombustionEngine(energyConsumptionForLinkInJoule);
			}
		} else {
			energyConsumptionForLinkInJoule=engineECM.getEnergyConsumptionForLinkInJoule(link, averageSpeedDriven);
			useCombustionEngine(energyConsumptionForLinkInJoule);
		}
		
		return energyConsumptionForLinkInJoule;
	}
	
	// e.g. use this method to log fuel consumption
	abstract void useCombustionEngine(double energyConsumptionInJoule);
	
}
