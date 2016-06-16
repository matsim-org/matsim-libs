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

import playground.michalm.ev.data.Battery;


public class ETaxi
    extends VehicleImpl
{
    private final Battery battery;//at start


    public ETaxi(Vehicle vehicle, Battery battery)
    {
        super(vehicle.getId(), vehicle.getStartLink(), vehicle.getCapacity(), vehicle.getT0(),
                vehicle.getT1());
        this.battery = battery;
    }


    public Battery getBattery()
    {
        return battery;
    }
}
