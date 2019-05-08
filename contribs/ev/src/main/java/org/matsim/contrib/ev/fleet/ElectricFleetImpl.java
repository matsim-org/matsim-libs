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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

public class ElectricFleetImpl implements ElectricFleet {
	public static ElectricFleet create(ElectricFleetSpecification fleetSpecification,
			DriveEnergyConsumption.Factory driveConsumptionFactory,
			AuxEnergyConsumption.Factory auxConsumptionFactory) {
		ElectricFleetImpl fleet = new ElectricFleetImpl();
		fleetSpecification.getVehicleSpecifications()
				.values()
				.stream()
				.map(s -> new ElectricVehicleImpl(s, driveConsumptionFactory.create(s),
						auxConsumptionFactory == null ? null : auxConsumptionFactory.create(s)))
				.forEach(fleet::addElectricVehicle);
		return fleet;
	}

	private final Map<Id<ElectricVehicle>, ElectricVehicle> electricVehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<ElectricVehicle>, ElectricVehicle> getElectricVehicles() {
		return Collections.unmodifiableMap(electricVehicles);
	}

	public void addElectricVehicle(ElectricVehicle ev) {
		electricVehicles.put(ev.getId(), ev);
	}
}
