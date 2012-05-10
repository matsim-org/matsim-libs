/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.taxi.wal;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.vrp.taxi.TaxiSimEngine;


public class WalVehicleLogic
{
    private TaxiSimEngine taxiSimEngine;
    private Vehicle vrpVehicle;


    private void scheduleNextTask(double now)
    {
        Schedule schedule = vrpVehicle.getSchedule();
        ScheduleStatus status = schedule.getStatus();

        if (status != ScheduleStatus.UNPLANNED) {
            throw new RuntimeException("Status is UNPLANNED");
        }

        if (status == ScheduleStatus.STARTED) {
            taxiSimEngine.updateAndOptimizeBeforeNextTask(vrpVehicle, now);
        }

        Task task = schedule.nextTask();
        status = schedule.getStatus();// REFRESH status!!!
    }
}
