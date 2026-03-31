/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleHouseholdMapping
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package org.matsim.households;

import java.util.Map;

import jakarta.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.vehicles.Vehicle;

/**
 * Tiny helper to get the household associated with a vehicle's id.
 *
 * @author dgrether
 *
 */
public class VehicleHouseholdMapping {

	private final Map<Id<Vehicle>, Household> vhMap = new IdMap<>(Vehicle.class);

	public VehicleHouseholdMapping(Households hhs) {
		this.reinitialize(hhs);
	}

	public void reinitialize(Households hhs) {
		this.vhMap.clear();
		for (Household h : hhs.getHouseholds().values()) {
			for (Id<Vehicle> vehicle : h.getVehicleIds()) {
				this.vhMap.put(vehicle, h);
			}
		}
	}

	public @Nullable Household getHousehold(Id<Vehicle> vehicleId) {
		return this.vhMap.get(vehicleId);
	}

}
