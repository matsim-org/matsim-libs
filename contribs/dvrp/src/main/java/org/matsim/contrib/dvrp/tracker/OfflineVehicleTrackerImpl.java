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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.contrib.dvrp.data.schedule.DriveTask;


public class OfflineVehicleTrackerImpl
    implements OfflineVehicleTracker
{
    private final double plannedEndTime;


    public OfflineVehicleTrackerImpl(DriveTask driveTask)
    {
        this.plannedEndTime = driveTask.getEndTime();
    }


    @Override
    public double calculateCurrentDelay(double currentTime)
    {
        return Math.max(0, currentTime - plannedEndTime);
    }


    @Override
    public double predictEndTime(double currentTime)
    {
        return Math.max(plannedEndTime, currentTime);
    }


    @Override
    public double getPlannedEndTime()
    {
        return plannedEndTime;
    }
}
