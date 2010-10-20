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
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DumbScenarioFleetInitializer implements FleetInitializer {

	@Override
	public LinkedListValueHashMap<Id, Vehicle> getVehicles(Set<Id> personIds, EnergyStateMaintainer energyStateMaintainer) {
		LinkedListValueHashMap<Id, Vehicle> result = new LinkedListValueHashMap<Id, Vehicle>();

		Iterator<Id> iter = personIds.iterator();

		while (iter.hasNext()) {
			Id personId = iter.next();

			PlugInHybridElectricVehicle phev = getInitializedPHEV(energyStateMaintainer);

			result.putAndSetBackPointer(personId, phev);
		}

		return result;
	}

	private PlugInHybridElectricVehicle getInitializedPHEV(EnergyStateMaintainer energyStateMaintainer) {
		PlugInHybridElectricVehicle phev = new PlugInHybridElectricVehicle(energyStateMaintainer, new IdImpl(1));
		double oneKWH = 1000.0 / 3600.0;
		phev.setBatterySizeInJoule(10 * oneKWH);
		phev.setBatteryMinThresholdInJoule(phev.getBatterySizeInJoule() * 0.035);
		phev.setCurrentBatteryChargeInJoule(phev.getBatterySizeInJoule());
		return phev;
	}

}
