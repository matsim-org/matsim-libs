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


public class VrpDynLegs
{
    public static VrpDynLeg createLegWithOfflineTracker(DriveTask driveTask)
    {
        VrpDynLeg leg = new VrpDynLeg(driveTask.getPath());

        TaskTracker tracker = new OfflineTaskTracker(driveTask);
        driveTask.setTaskTracker(tracker);
        leg.initTracking(tracker);

        return leg;
    }


    public static VrpDynLeg createLegWithOnlineTracker(DriveTask driveTask,
            VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer)
    {
        VrpDynLeg leg = new VrpDynLeg(driveTask.getPath());

        OnlineDriveTaskTrackerImpl tracker = new OnlineDriveTaskTrackerImpl(driveTask, leg,
                optimizer, timer);
        driveTask.setTaskTracker(tracker);
        leg.initTracking(tracker);

        return leg;
    }


    public static interface LegCreator
    {
        VrpDynLeg createLeg(DriveTask driveTask);
    }


    public static final LegCreator LEG_WITH_OFFLINE_TRACKER_CREATOR = new LegCreator() {
        public VrpDynLeg createLeg(DriveTask driveTask)
        {
            return createLegWithOfflineTracker(driveTask);
        };
    };


    public static LegCreator createLegWithOnlineTrackerCreator(
            final VrpOptimizerWithOnlineTracking optimizer, final MobsimTimer timer)
    {
        return new LegCreator() {
            @Override
            public VrpDynLeg createLeg(DriveTask driveTask)
            {
                return createLegWithOnlineTracker(driveTask, optimizer, timer);
            }
        };
    }
}
