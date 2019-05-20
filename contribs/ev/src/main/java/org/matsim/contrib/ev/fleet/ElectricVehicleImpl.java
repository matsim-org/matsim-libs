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

import com.google.common.collect.ImmutableList;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

public class ElectricVehicleImpl implements ElectricVehicle {
    public static final String DEFAULT_VEHICLE_TYPE = "defaultVehicleType";

    private final ElectricVehicleSpecification vehicleSpecification;
    private final Battery battery;

    private DriveEnergyConsumption driveEnergyConsumption;
    private AuxEnergyConsumption auxEnergyConsumption;


    public static ElectricVehicle create(ElectricVehicleSpecification vehicleSpecification, DriveEnergyConsumption.Factory driveFactory, AuxEnergyConsumption.Factory auxFactory) {
        ElectricVehicleImpl ev = new ElectricVehicleImpl(vehicleSpecification);
        ev.driveEnergyConsumption = driveFactory == null ? null : driveFactory.create(ev);
        ev.auxEnergyConsumption = auxFactory == null ? null : auxFactory.create(ev);

        return ev;
    }


    private ElectricVehicleImpl(ElectricVehicleSpecification vehicleSpecification) {
        this.vehicleSpecification = vehicleSpecification;
        battery = new BatteryImpl(vehicleSpecification.getBatteryCapacity(), vehicleSpecification.getInitialSoc());

    }

    @Override
    public Id<ElectricVehicle> getId() {
        return vehicleSpecification.getId();
    }

    @Override
    public Battery getBattery() {
        return battery;
    }

    @Override
    public String getVehicleType() {
        return vehicleSpecification.getVehicleType();
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
}
