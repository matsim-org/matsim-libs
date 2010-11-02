/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionModelLAV.java
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

package playground.wrashid.PSF2.pluggable.energyConsumption;

import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

public class EnergyConsumptionModelLAV implements EnergyConsumptionModel {

	EnergyConsumptionTable energyConsumptionTable;
	
	public EnergyConsumptionModelLAV(String fileWithModelData){
		energyConsumptionTable=new EnergyConsumptionTable(fileWithModelData);
	}
	
	public double getEnergyConsumptionForLinkInJoule(Vehicle vehicle, double timeSpentOnLink, Link link) {
		return energyConsumptionTable.getEnergyConsumptionInJoule(vehicle.getVehicleClassId(), Vehicle.getAverageSpeedOfVehicleOnLink(timeSpentOnLink, link), link.getFreespeed(), link.getLength());
	}

}
