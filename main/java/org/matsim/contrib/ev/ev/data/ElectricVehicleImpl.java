/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.ev.data;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.ev.discharging.DriveEnergyConsumption;

import java.util.Collections;
import java.util.List;

public class ElectricVehicleImpl implements ElectricVehicle {
	public static final String DEFAULTVEHICLETYPE = "defaultVehicleType";
	private final Id<ElectricVehicle> vehicleId;
	private Battery battery;// not final -- can be swapped
    private final List<String> chargingTypes;
	private final String vehicleType;

	private DriveEnergyConsumption driveEnergyConsumption;
	private AuxEnergyConsumption auxEnergyConsumption;

    public ElectricVehicleImpl(Id<ElectricVehicle> vehicleId, Battery battery) {
        this.vehicleId = vehicleId;
        this.battery = battery;
        this.chargingTypes = Collections.singletonList(ChargerImpl.DEFAULT_CHARGER_TYPE);
		this.vehicleType = DEFAULTVEHICLETYPE;
    }

	public ElectricVehicleImpl(Id<ElectricVehicle> vehicleId, Battery battery, List<String> chargingTypes, String vehicleType) {
        this.vehicleId = vehicleId;
        this.battery = battery;
        this.chargingTypes = chargingTypes;
		this.vehicleType = vehicleType;
    }

	@Override
	public Id<ElectricVehicle> getId() {
		return vehicleId;
	}

	@Override
	public Battery getBattery() {
		return battery;
	}

    @Override
    public List<String> getChargingTypes() {
        return chargingTypes;
    }

	@Override
	public String getVehicleType() {
		return vehicleType;
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
	public Battery swapBattery(Battery battery) {
		Battery old = this.battery;
		this.battery = battery;
		return old;
	}

	@Override
	public void setDriveEnergyConsumption(DriveEnergyConsumption driveEnergyConsumption) {
		this.driveEnergyConsumption = driveEnergyConsumption;
	}

	@Override
	public void setAuxEnergyConsumption(AuxEnergyConsumption auxEnergyConsumption) {
		this.auxEnergyConsumption = auxEnergyConsumption;
	}
}
