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
import com.google.inject.Singleton;

/**
 * By default, this class works only for DvrpModes that are bound via a Multibinder:
 * <p>
 * Multibinder.newSetBinder(binder(), DvrpMode.class).addBinding().toInstance(DvrpModes.mode(getMode()));
 * <p>
 * However, a custom DvrpVehicleLookup can be created for selected DvrpModes
 *
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

	private Map<Id<DvrpVehicle>, VehicleAndMode> vehicleLookupMap;
	private final Set<DvrpMode> dvrpModes;

	@Inject
	private Injector injector;

	@Inject
	public DvrpVehicleLookup(Set<DvrpMode> dvrpModes) {
		this.dvrpModes = dvrpModes;
	}

	public VehicleAndMode lookupVehicleAndMode(Id<DvrpVehicle> id) {
		if (vehicleLookupMap == null) {
			vehicleLookupMap = new HashMap<>();
			for (DvrpMode m : dvrpModes) {
				for (DvrpVehicle v : injector.getInstance(Key.get(Fleet.class, m)).getVehicles().values()) {
					if (vehicleLookupMap.put(v.getId(), new VehicleAndMode(v, m.value())) != null) {
						throw new RuntimeException("Two DvrpVehicles with the same id: " + v.getId());
					}
				}
			}
		}

		return vehicleLookupMap.get(id);
	}

	public DvrpVehicle lookupVehicle(Id<DvrpVehicle> id) {
		VehicleAndMode vehicleAndMode = lookupVehicleAndMode(id);
		return vehicleAndMode == null ? null : vehicleAndMode.vehicle;
	}
}
