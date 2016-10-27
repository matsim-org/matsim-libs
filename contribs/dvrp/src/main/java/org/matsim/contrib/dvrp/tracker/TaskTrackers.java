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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.core.mobsim.framework.MobsimTimer;


/**
 * General assumptions:
 * <p></p>
 * An offline tracker knows/uses only the corresponding task and the schedule
 * <p></p>
 * An online tracker knows/uses also the corresponding DynAction
 */
public class TaskTrackers
{
    public static void initOnlineDriveTaskTracking(DriveTask driveTask, VrpLeg vrpDynLeg,
            VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer)
    {
        OnlineDriveTaskTracker onlineTracker = new OnlineDriveTaskTrackerImpl(driveTask, vrpDynLeg,
                optimizer, timer);

        driveTask.initTaskTracker(onlineTracker);
        vrpDynLeg.initOnlineTracking(onlineTracker);
    }


    public static void initOnlineStayTaskTracking(StayTask stayTask, final DynActivity dynActivity)
    {
        stayTask.initTaskTracker(new TaskTracker() {
            @Override
            public double predictEndTime()
            {
                return dynActivity.getEndTime();
            }
        });
    }


    public static void initOfflineTaskTracking(final Task task, final MobsimTimer timer)
    {
        task.initTaskTracker(new TaskTracker() {
            @Override
            public double predictEndTime()
            {
                return TaskTrackers.predictEndTimeOffline(task, timer.getTimeOfDay());
            }
        });
    }


    public static double predictEndTime(Task task, double currentTime)
    {
        if (task.getStatus() != TaskStatus.STARTED) {
            throw new IllegalStateException();
        }

        TaskTracker tracker = task.getTaskTracker();
        return tracker != null ? //
                tracker.predictEndTime() : predictEndTimeOffline(task, currentTime);
    }


    private static double predictEndTimeOffline(Task task, double currentTime)
    {
        return Math.max(task.getEndTime(), currentTime);
    }
}
