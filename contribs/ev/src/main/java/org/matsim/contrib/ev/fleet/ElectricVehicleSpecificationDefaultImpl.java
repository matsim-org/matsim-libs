/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
final class ElectricVehicleSpecificationDefaultImpl implements ElectricVehicleSpecification {

	private final Vehicle matsimVehicle;

	ElectricVehicleSpecificationDefaultImpl( Vehicle matsimVehicle ) {
		this.matsimVehicle = matsimVehicle;
		//provided per vehicle type (in engine info)
		Preconditions.checkArgument(getInitialSoc() >= 0 && getInitialSoc() <= 1, "Invalid initialCharge or batteryCapacity of vehicle: %s", getId());
	}

	@Override
	public Id<Vehicle> getId() {
		return matsimVehicle.getId();
	}

	@Override
	public Vehicle getMatsimVehicle() {
		return matsimVehicle;
	}

	@Override
	public ImmutableList<String> getChargerTypes() {
		var engineInfo = matsimVehicle.getType().getEngineInformation();
		return ImmutableList.copyOf((Collection<String>)engineInfo.getAttributes().getAttribute( ElectricFleetUtils.CHARGER_TYPES ) );
	}

	@Override
	public double getInitialSoc() {
		return (double)matsimVehicle.getAttributes().getAttribute( ElectricFleetUtils.INITIAL_SOC );
	}

	@Override
	public double getBatteryCapacity() {
		var engineInfo = matsimVehicle.getType().getEngineInformation();
		return VehicleUtils.getEnergyCapacity(engineInfo) * EvUnits.J_PER_kWh;
	}
}
