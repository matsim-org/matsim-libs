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

package org.matsim.contrib.dvrp.extensions.electric;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.model.Depot;
import org.matsim.contrib.dvrp.data.model.impl.VehicleImpl;


public class ElectricVehicleImpl
    extends VehicleImpl
    implements ElectricVehicle
{
    private Battery battery;


    public ElectricVehicleImpl(Id id, String name, Depot depot, double capacity, double t0,
            double t1, double timeLimit, Battery battery)
    {
        super(id, name, depot, capacity, t0, t1, timeLimit);
        this.battery = battery;
    }


    @Override
    public Battery getBattery()
    {
        return battery;
    }


    @Override
    public void setBattery(Battery battery)
    {
        this.battery = battery;
    }
}
