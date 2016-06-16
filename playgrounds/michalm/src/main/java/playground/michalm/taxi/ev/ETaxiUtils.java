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
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiData;

import playground.michalm.ev.UnitConversionRatios;
import playground.michalm.ev.charging.PartialFastChargingWithQueueingLogic;
import playground.michalm.ev.data.*;
import playground.michalm.ev.discharging.EnergyConsumptions;


public class ETaxiUtils
{
    public static void initEvData(TaxiData taxiData, EvData evData)
    {
        // TODO reduce charging speed in winter
        for (Charger c : evData.getChargers().values()) {
            new PartialFastChargingWithQueueingLogic(c);
        }

        // TODO variable AUX -- depends on weather etc...
        // TODO add the Leaf's consumption model for driving 
        double driveRate = 150. * 3.6; //15 kWh / 100 km == 150 Wh/km; converted into J/m
        double auxPower = 500; //0.5 kW

        double batteryCapacity = 20 * UnitConversionRatios.J_PER_kWh;
        double initialSoc = 0.8 * 20 * UnitConversionRatios.J_PER_kWh;

        for (Vehicle v : taxiData.getVehicles().values()) {
            ElectricVehicleImpl ev = new ElectricVehicleImpl(
                    new BatteryImpl(batteryCapacity, initialSoc));
            ev.setDriveEnergyConsumption((link, travelTime) -> EnergyConsumptions
                    .consumeFixedDriveEnergy(ev, driveRate, link));
            ev.setAuxEnergyConsumption(
                    (period) -> consumeFixedAuxEnergyWhenStarted(ev, v, auxPower, period));

            evData.addElectricVehicle(Id.createVehicleId(v.getId()), ev);
        }
    }


    public static void consumeFixedAuxEnergyWhenStarted(ElectricVehicle ev, Vehicle taxi,
            double auxPower, double period)
    {
        if (taxi.getSchedule().getStatus() == ScheduleStatus.STARTED) {
            ev.getBattery().discharge(auxPower * period);
        }
    }
}
