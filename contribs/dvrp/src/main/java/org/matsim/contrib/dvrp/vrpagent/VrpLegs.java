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
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.core.mobsim.framework.MobsimTimer;


public class VrpLegs
{
    public static VrpLeg createLegWithOfflineTracker(DriveTask driveTask, MobsimTimer timer)
    {
        VrpLeg leg = new VrpLeg(driveTask.getPath());
        TaskTrackers.initOfflineTaskTracking(driveTask, timer);
        return leg;
    }


    public static VrpLeg createLegWithOnlineTracker(DriveTask driveTask,
            VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer)
    {
        VrpLeg leg = new VrpLeg(driveTask.getPath());
        TaskTrackers.initOnlineDriveTaskTracking(driveTask, leg, optimizer, timer);
        return leg;
    }


    public static interface LegCreator
    {
        VrpLeg createLeg(DriveTask driveTask);
    }


    public static LegCreator createLegWithOfflineTrackerCreator(final MobsimTimer timer)
    {
        return new LegCreator() {
            @Override
            public VrpLeg createLeg(DriveTask driveTask)
            {
                return createLegWithOfflineTracker(driveTask, timer);
            }
        };
    }


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
