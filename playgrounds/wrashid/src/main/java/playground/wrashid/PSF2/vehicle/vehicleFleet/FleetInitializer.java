/* *********************************************************************** *
 * project: org.matsim.*
 * FleetInitializer.java
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

import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public interface FleetInitializer {

	/**
	 * return value: personId, vehicle
	 * @param personIds
	 * @param energyStateMaintainer
	 * @return
	 */
	public LinkedListValueHashMap<Id, Vehicle> getVehicles(Set<Id> personIds, EnergyStateMaintainer energyStateMaintainer);
	
}
