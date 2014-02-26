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

package playground.michalm.taxi.util;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


public class TaxiUtils
{
    public static final Predicate<Vehicle> IS_IDLE_PREDICATE = new Predicate<Vehicle>() {
        public boolean apply(Vehicle vehicle)
        {
            return isIdle(vehicle);
        }
    };


    public static Iterable<Vehicle> filterIdleVehicles(Iterable<Vehicle> vehicles)
    {
        return Iterables.filter(vehicles, IS_IDLE_PREDICATE);
    }


    public static boolean isIdle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(vehicle);

        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxiTask currentTask = schedule.getCurrentTask();

        if (Schedules.isLastTask(currentTask)
                && currentTask.getTaxiTaskType() == TaxiTaskType.WAIT_STAY) {
            return true;
        }

        return false;
    }


    public static boolean isCurrentTaskDelayed(Schedule<TaxiTask> schedule, double now)
    {
        TaxiTask currentTask = schedule.getCurrentTask();
        double delay = now - currentTask.getEndTime();

        if (delay < 0) {
            return false;
        }
        else {
            throw new IllegalStateException();
        }
    }


    public static int countIdleVehicles(Iterable<Vehicle> vehicles)
    {
        return Iterables.size(Iterables.filter(vehicles, IS_IDLE_PREDICATE));
    }
}
