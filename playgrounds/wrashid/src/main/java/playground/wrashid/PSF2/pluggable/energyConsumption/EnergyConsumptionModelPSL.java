/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionModelPSL.java
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

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF.data.energyConsumption.AverageEnergyConsumptionGalus;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.GeneralLogObject;

public class EnergyConsumptionModelPSL implements EnergyConsumptionModel {

	AverageEnergyConsumptionGalus phevEnergyConsumptionModel;
	GeneralLogObject generalPSFLog;
	
	public EnergyConsumptionModelPSL(GeneralLogObject generalPSFLog){
		phevEnergyConsumptionModel=new AverageEnergyConsumptionGalus();
		this.generalPSFLog=generalPSFLog;
	}
	
	
	@Override
	public double getEnergyConsumptionForLinkInJoule(Vehicle vehicle, double timeSpentOnLink, Link link) {
		if (vehicle.getVehicleClassId()==new IdImpl("1")){
			// NOTE: phevs must have class Id one in this case
			return phevEnergyConsumptionModel.getEnergyConsumption(Vehicle.getAverageSpeedOfVehicleOnLink(timeSpentOnLink, link), link.getLength());
		} else {
			ArrayList<String> logGroup = generalPSFLog.createArrayListForNewLogGroup(EnergyConsumptionModelPSL.class.getName());
			logGroup.add("must implement energy consumption of this vehicle class, before using this model: vehicleClassId=" + vehicle.getVehicleClassId());
			return 0;
		}
	}

}
