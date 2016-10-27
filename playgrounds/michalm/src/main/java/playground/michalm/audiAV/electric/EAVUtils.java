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

package playground.michalm.audiAV.electric;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.schedule.*;

import playground.michalm.ev.data.*;
import playground.michalm.ev.discharging.*;
import playground.michalm.ev.discharging.OhdeSlaskiAuxEnergyConsumption.TemperatureProvider;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingLogic;


public class EAVUtils
{
    public static void initEvData(TaxiData taxiData, EvData evData)
    {
        TemperatureProvider tempProvider = () -> 20;// aux power about 1 kW at 20oC
        double chargingSpeedFactor = 1.; //full speed

        for (Charger c : evData.getChargers().values()) {
            new ETaxiChargingLogic(c, chargingSpeedFactor);
        }

        for (Vehicle v : taxiData.getVehicles().values()) {
            Ev ev = ((EvrpVehicle)v).getEv();
            ev.setDriveEnergyConsumption(new OhdeSlaskiDriveEnergyConsumption());
            ev.setAuxEnergyConsumption(new OhdeSlaskiAuxEnergyConsumption(ev, tempProvider,
                    EAVUtils::isServingCustomer));
            evData.addElectricVehicle(Id.createVehicleId(v.getId()), ev);
        }
    }


    private static boolean isServingCustomer(ElectricVehicle ev)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules
                .asTaxiSchedule( ((Ev)ev).getEvrpVehicle().getSchedule());
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        switch (schedule.getCurrentTask().getTaxiTaskType()) {
            case PICKUP:
            case OCCUPIED_DRIVE:
            case DROPOFF:
                return true;

            //TODO driving empty to cusomer is not handled yet (will be?)

            default:
                return false;
        }
    }
}
