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

package playground.michalm.taxi.scheduler;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;

import com.google.common.base.Predicate;


public class TaxiSchedulerUtils
{

    public static final Predicate<Vehicle> IS_IDLE = new Predicate<Vehicle>() {
        public boolean apply(Vehicle vehicle)
        {
            return TaxiSchedulerUtils.isIdle(vehicle);
        }
    };


    public static Predicate<Vehicle> createCanBeScheduled(final TaxiScheduler scheduler)
    {
        return new Predicate<Vehicle>() {
            public boolean apply(Vehicle vehicle)
            {
                return TaxiSchedulerUtils.canBeScheduled(vehicle, scheduler);
            }
        };
    }


    public static boolean isIdle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(vehicle);

        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxiTask currentTask = schedule.getCurrentTask();

        //schedule ends with: WAIT(==current)
        if (Schedules.isLastTask(currentTask)
                && currentTask.getTaxiTaskType() == TaxiTaskType.WAIT_STAY) {
            return true;
        }

        //schedule ends with: CHARGE(==current) + WAIT
        if (Schedules.isNextToLastTask(currentTask)
                && currentTask.getTaxiTaskType() == TaxiTaskType.CHARGE_STAY) {
            if ( (Schedules.getLastTask(schedule)).getTaxiTaskType() != TaxiTaskType.WAIT_STAY) {
                throw new IllegalStateException();
            }

            return true;
        }

        return false;
    }


    public static boolean canBeScheduled(Vehicle vehicle, TaxiScheduler scheduler)
    {
        return scheduler.getEarliestIdleness(vehicle) != null;
    }

}
