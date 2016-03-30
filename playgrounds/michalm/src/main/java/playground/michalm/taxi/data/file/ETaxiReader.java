/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.data.file;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.*;
import org.xml.sax.Attributes;

import playground.michalm.ev.*;
import playground.michalm.taxi.data.ETaxi;


public class ETaxiReader
    extends VehicleReader
{
    public ETaxiReader(Network network, VrpData data)
    {
        super(network, data);
    }


    @Override
    protected Vehicle createVehicle(Attributes atts)
    {
        Vehicle v = super.createVehicle(atts);
        double batteryCapacity = ReaderUtils.getDouble(atts, "battery_capacity", 20)
                * UnitConversionRatios.J_PER_kWh;
        double initialSoc = ReaderUtils.getDouble(atts, "initial_soc", 0.8 * 20)
                * UnitConversionRatios.J_PER_kWh;
        return new ETaxi(v, new BatteryImpl(batteryCapacity, initialSoc));
    }
}
