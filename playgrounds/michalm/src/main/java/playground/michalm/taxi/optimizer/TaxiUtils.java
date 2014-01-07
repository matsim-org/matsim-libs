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

package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.data.schedule.*;
import org.matsim.contrib.dvrp.data.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiUtils
{
    public static boolean isIdle(Schedule<TaxiTask> schedule, int time,
            boolean delayedWaitTaskAsNonIdle)
    {
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxiTask currentTask = schedule.getCurrentTask();

        switch (currentTask.getTaxiTaskType()) {
            case PICKUP_DRIVE://maybe in the future some diversion will be enabled....
            case PICKUP_STAY:
            case DROPOFF_DRIVE:
            case DROPOFF_STAY:
                return false;

            case CRUISE_DRIVE:
                // TODO this requires some analysis if a vehicle en route can be immediately
                // diverted or there is a lag (as in the case of WAIT);
                // how long is the lag??
                System.err
                        .println("Currently CRUISE cannot be interrupted, so the vehicle is considered BUSY...");
                return false;

            case WAIT_STAY:
                if (delayedWaitTaskAsNonIdle && isCurrentTaskDelayed(schedule, time)) {
                    return false;// assuming that the next task is a non-wait task
                }
        }

        // idle right now, but:
        // consider CLOSING (T1) time windows of the vehicle
        if (time >= Schedules.getActualT1(schedule)) {
            return false;
        }

        return true;
    }


    public static boolean isCurrentTaskDelayed(Schedule<TaxiTask> schedule, int time)
    {
        TaxiTask currentTask = schedule.getCurrentTask();
        int delay = time - currentTask.getEndTime();

        if (delay < 0) {
            return false;
        }

        if (currentTask.getTaxiTaskType() == TaxiTaskType.WAIT_STAY && delay >= 2) {
            // there can be a lag between a change in the schedule (WAIT->OTHER)
            // because activity ends (here, WAIT end) are handled only at the beginning of
            // a simulation step, i.e. ActivityEngine is before QNetsimEngine
            // According to some code analysis, the lag should not be larger than 1 second
            // TODO BTW. Is "ActivityEngine before QNetsimEngine" the only approach???
            System.err.println("TaxiUtils.isCurrentTaskDelayed(Schedule schedule, int time): "
                    + "This is very unlikely! I am just curious if this ever happens:-)");
        }

        return true;
    }
}
