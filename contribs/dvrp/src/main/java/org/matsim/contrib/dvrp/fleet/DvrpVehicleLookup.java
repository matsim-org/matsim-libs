/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;

import com.google.common.base.Verify;
import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
@Singleton
public class DvrpVehicleLookup {
	public static class VehicleAndMode {
		public final DvrpVehicle vehicle;
		public final String mode;

		private VehicleAndMode(DvrpVehicle vehicle, String mode) {
			this.vehicle = vehicle;
			this.mode = mode;
		}
	}

	private final IdMap<DvrpVehicle, VehicleAndMode> vehicleLookupMap = new IdMap<>(DvrpVehicle.class);

	void addVehicle(String mode, DvrpVehicle vehicle) {
		Verify.verify(vehicleLookupMap.put(vehicle.getId(), new VehicleAndMode(vehicle, mode)) == null);
	}

	void removeVehicle(String mode, Id<DvrpVehicle> vehicleId) {
		Verify.verify(vehicleLookupMap.remove(vehicleId).mode.equals(mode));
	}

	public VehicleAndMode lookupVehicleAndMode(Id<DvrpVehicle> id) {
		return vehicleLookupMap.get(id);
	}

	public DvrpVehicle lookupVehicle(Id<DvrpVehicle> id) {
		VehicleAndMode vehicleAndMode = lookupVehicleAndMode(id);
		return vehicleAndMode == null ? null : vehicleAndMode.vehicle;
	}
}
