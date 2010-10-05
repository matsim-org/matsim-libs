/* *********************************************************************** *
 * project: org.matsim.*
 * DumbScenarioFleetInitializer.java
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.FleetInitializer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

public class DumbScenarioFleetInitializer implements FleetInitializer {

	@Override
	public HashMap<Id, Vehicle> getVehicles(Set<Id> personIds, EnergyStateMaintainer energyStateMaintainer) {
		HashMap<Id, Vehicle> result=new HashMap<Id, Vehicle>();
		
		Iterator<Id> iter=personIds.iterator();
		
		while (iter.hasNext()){
			Id personId=iter.next();
			
			result.put(personId, new PlugInHybridElectricVehicle(energyStateMaintainer, new IdImpl(1)));
		}
		
		return result;
	}

}
