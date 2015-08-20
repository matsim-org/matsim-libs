/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.data;

import org.matsim.contrib.dvrp.data.*;

import playground.michalm.ev.*;


public class ETaxi
    extends VehicleImpl
    implements ElectricVehicle
{
    private final Battery battery;
    private final DriveEnergyConsumption driveEnergyConsumption;
    private final AuxEnergyConsumption auxEnergyConsumption;


    public ETaxi(Vehicle vehicle, Battery battery,
            DriveEnergyConsumption driveEnergyConsumption,
            AuxEnergyConsumption auxEnergyConsumption)
    {
        super(vehicle.getId(), vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getT0(),
                vehicle.getT1());

        this.battery = battery;
        this.driveEnergyConsumption = driveEnergyConsumption;
        this.auxEnergyConsumption = auxEnergyConsumption;
    }


    @Override
    public Battery getBattery()
    {
        return battery;
    }


    @Override
    public DriveEnergyConsumption getDriveEnergyConsumption()
    {
        return driveEnergyConsumption;
    }


    @Override
    public AuxEnergyConsumption getAuxEnergyConsumption()
    {
        return auxEnergyConsumption;
    }
}
