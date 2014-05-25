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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.core.mobsim.framework.MobsimTimer;


public class VrpLegs
{
    public static VrpLeg createLegWithOfflineTracker(DriveTask driveTask)
    {
        VrpLeg leg = new VrpLeg(driveTask.getPath());

        TaskTracker tracker = new OfflineTaskTracker(driveTask);
        driveTask.setTaskTracker(tracker);
        leg.initTracking(tracker);

        return leg;
    }


    public static VrpLeg createLegWithOnlineTracker(DriveTask driveTask,
            VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer)
    {
        VrpLeg leg = new VrpLeg(driveTask.getPath());

        OnlineDriveTaskTrackerImpl tracker = new OnlineDriveTaskTrackerImpl(driveTask, leg,
                optimizer, timer);
        driveTask.setTaskTracker(tracker);
        leg.initTracking(tracker);

        return leg;
    }


    public static interface LegCreator
    {
        VrpLeg createLeg(DriveTask driveTask);
    }


    public static final LegCreator LEG_WITH_OFFLINE_TRACKER_CREATOR = new LegCreator() {
        public VrpLeg createLeg(DriveTask driveTask)
        {
            return createLegWithOfflineTracker(driveTask);
        };
    };


    public static LegCreator createLegWithOnlineTrackerCreator(
            final VrpOptimizerWithOnlineTracking optimizer, final MobsimTimer timer)
    {
        return new LegCreator() {
            @Override
            public VrpLeg createLeg(DriveTask driveTask)
            {
                return createLegWithOnlineTracker(driveTask, optimizer, timer);
            }
        };
    }
}
