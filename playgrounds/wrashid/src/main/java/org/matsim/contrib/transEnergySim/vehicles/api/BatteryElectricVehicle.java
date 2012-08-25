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

import playground.wrashid.lib.DebugLib;

/**
 * vehicle has only battery (no combustion engine)
 * 
 * @author wrashid
 * 
 */
public abstract class BatteryElectricVehicle extends VehicleWithBattery {

	/**
	 * as electric vehicles can run out of battery during the simulation, this
	 * has also to be taken into account
	 */
	private boolean didRunOutOfBattery=false;
	
	@Override
	public void useBattery(double energyConsumptionInJoule){
		super.useBattery(energyConsumptionInJoule);
		
		if (socInJoules<0){
			didRunOutOfBattery=true;
		}
	}
	
	
	
	public void updateEnergyUse(Link link, double averageSpeedDriven){
		double energyConsumptionForLinkInJoule = electricDriveEnergyConsumptionModel.getEnergyConsumptionForLinkInJoule(link, averageSpeedDriven);
		
		useBattery(energyConsumptionForLinkInJoule);
	}
			
	
	
}
