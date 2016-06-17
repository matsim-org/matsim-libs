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

import playground.michalm.ev.data.*;


public class ETaxi
    extends VehicleImpl
{
    private final ElectricVehicle ev;


    public ETaxi(Vehicle vehicle, double batteryCapacity, double initialSoc)
    {
        super(vehicle.getId(), vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getT0(),
                vehicle.getT1());
        ev = new ElectricVehicleImpl(new BatteryImpl(batteryCapacity, initialSoc));
    }


    public ElectricVehicle getEv()
    {
        return ev;
    }
}