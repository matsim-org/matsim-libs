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
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

final class ElectricVehicleDefaultImpl implements ElectricVehicle {

	private final ElectricVehicleSpecification vehicleSpecification;
	private final Battery battery;

	DriveEnergyConsumption driveEnergyConsumption;
	AuxEnergyConsumption auxEnergyConsumption;
	ChargingPower chargingPower;

	ElectricVehicleDefaultImpl( ElectricVehicleSpecification vehicleSpecification ) {
		this.vehicleSpecification = vehicleSpecification;
		battery = new BatteryDefaultImpl(vehicleSpecification.getBatteryCapacity(), vehicleSpecification.getInitialCharge());
	}

	@Override
	public Id<Vehicle> getId() {
		return vehicleSpecification.getId();
	}

	@Override
	public Battery getBattery() {
		return battery;
	}

	@Override
	public ElectricVehicleSpecification getVehicleSpecification() {
		return vehicleSpecification;
	}

	@Override
	public ImmutableList<String> getChargerTypes() {
		return vehicleSpecification.getChargerTypes();
	}

	@Override
	public DriveEnergyConsumption getDriveEnergyConsumption() {
		return driveEnergyConsumption;
	}

	@Override
	public AuxEnergyConsumption getAuxEnergyConsumption() {
		return auxEnergyConsumption;
	}

	@Override
	public ChargingPower getChargingPower() {
		return chargingPower;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("vehicleSpecification", vehicleSpecification)
				.add("battery", battery)
				.add("driveEnergyConsumption", driveEnergyConsumption.getClass())
				.add("auxEnergyConsumption", auxEnergyConsumption.getClass())
				.add("chargingPower", chargingPower.getClass())
				.toString();
	}
}
