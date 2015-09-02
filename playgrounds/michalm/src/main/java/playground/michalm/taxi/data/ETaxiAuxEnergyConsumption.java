/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.ev.AuxEnergyConsumption;


public class ETaxiAuxEnergyConsumption
    implements AuxEnergyConsumption
{
    private final ETaxi taxi;
    private final double auxPower;


    public ETaxiAuxEnergyConsumption(ETaxi taxi, double auxPower)
    {
        this.taxi = taxi;
        this.auxPower = auxPower;
    }


    @Override
    public void useEnergy(double period)
    {
        if (taxi.getSchedule().getStatus() == ScheduleStatus.STARTED) {
            taxi.getBattery().discharge(auxPower * period);
        }
    }
}
