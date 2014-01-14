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

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.schedule.DriveTask;
import org.matsim.contrib.dvrp.tracker.*;


public class VrpDynLegs
{
    public static VrpDynLeg createLegWithOfflineVehicleTracker(DriveTask driveTask)
    {
        OfflineVehicleTracker tracker = new OfflineVehicleTrackerImpl(driveTask);
        driveTask.setVehicleTracker(tracker);
        return new VrpDynLeg(driveTask.getPath());
    }


    public static VrpDynLeg createLegWithOnlineVehicleTracker(DriveTask driveTask,
            VrpSimEngine vrpSimEngine)
    {
        OnlineVehicleTracker tracker = new OnlineVehicleTrackerImpl(driveTask, vrpSimEngine);
        driveTask.setVehicleTracker(tracker);
        return new VrpDynLeg(tracker);
    }
}
