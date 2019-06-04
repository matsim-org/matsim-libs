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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.run.DvrpMode;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
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

	private final Map<Id<DvrpVehicle>, VehicleAndMode> vehicleLookupMap;

	public DvrpVehicleLookup(Map<Id<DvrpVehicle>, VehicleAndMode> vehicleLookupMap) {
		this.vehicleLookupMap = vehicleLookupMap;
	}

	public VehicleAndMode lookupVehicleAndMode(Id<DvrpVehicle> id) {
		return vehicleLookupMap.get(id);
	}

	public DvrpVehicle lookupVehicle(Id<DvrpVehicle> id) {
		VehicleAndMode vehicleAndMode = lookupVehicleAndMode(id);
		return vehicleAndMode == null ? null : vehicleAndMode.vehicle;
	}

	public static class DvrpVehicleLookupProvider implements Provider<DvrpVehicleLookup> {
		@Inject
		private Injector injector;

		@Inject
		private Set<DvrpMode> dvrpModes;

		@Override
		public DvrpVehicleLookup get() {
			Map<Id<DvrpVehicle>, VehicleAndMode> vehicleLookupMap = new HashMap<>();
			for (DvrpMode m : dvrpModes) {
				for (DvrpVehicle v : injector.getInstance(Key.get(Fleet.class, m)).getVehicles().values()) {
					if (vehicleLookupMap.put(v.getId(), new VehicleAndMode(v, m.value())) != null) {
						throw new RuntimeException("Two DvrpVehicles with the same id: " + v.getId());
					}
				}
			}
			return new DvrpVehicleLookup(vehicleLookupMap);
		}
	}
}
