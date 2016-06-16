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

package playground.michalm.taxi.ev;

import org.matsim.api.core.v01.Id;

import playground.michalm.ev.charging.PartialFastChargingWithQueueingLogic;
import playground.michalm.ev.data.*;
import playground.michalm.ev.discharging.EnergyConsumptions;
import playground.michalm.taxi.data.*;


public class ETaxiUtils
{
    public static void initEvData(ETaxiData eTaxiData, EvData evData)
    {
        // TODO reduce charging speed in winter
        for (Charger c : evData.getChargers().values()) {
            new PartialFastChargingWithQueueingLogic(c);
        }

        // TODO variable AUX -- depends on weather etc...
        // TODO add the Leaf's consumption model for driving 

        double driveRate = 150. * 3.6; //15 kWh / 100 km == 150 Wh/km; converted into J/m
        double auxPower = 500; //0.5 kW 

        for (ETaxi et : eTaxiData.getETaxis().values()) {
            ElectricVehicleImpl ev = new ElectricVehicleImpl(et.getBattery());
            ev.setDriveEnergyConsumption(
                    EnergyConsumptions.createFixedDriveEnergyConsumption(ev, driveRate));
            ev.setAuxEnergyConsumption(new ETaxiAuxEnergyConsumption(et, auxPower));
            evData.addElectricVehicle(Id.createVehicleId(et.getId()), ev);
        }
    }
}
