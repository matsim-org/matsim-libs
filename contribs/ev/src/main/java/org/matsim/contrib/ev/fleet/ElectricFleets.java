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

package org.matsim.contrib.ev.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

import com.google.common.collect.ImmutableMap;

public class ElectricFleets {
	public static ElectricFleet createDefaultFleet(ElectricFleetSpecification fleetSpecification,
			DriveEnergyConsumption.Factory driveConsumptionFactory,
			AuxEnergyConsumption.Factory auxConsumptionFactory) {
		ImmutableMap<Id<ElectricVehicle>, ? extends ElectricVehicle> vehicles = fleetSpecification.getVehicleSpecifications()
				.values()
				.stream()
				.map(s -> ElectricVehicleImpl.create(s, driveConsumptionFactory, auxConsumptionFactory))
				.collect(ImmutableMap.toImmutableMap(ElectricVehicle::getId, v -> v));
		return () -> vehicles;
	}
}
