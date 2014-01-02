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

package pl.poznan.put.vrp.dynamic.data.online;

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public class OfflineVehicleTracker
    implements VehicleTracker
{
    private final DriveTask driveTask;
    private final int initialEndTime;


    public OfflineVehicleTracker(DriveTask driveTask)
    {
        this.driveTask = driveTask;
        this.initialEndTime = driveTask.getEndTime();
    }


    @Override
    public DriveTask getDriveTask()
    {
        return driveTask;
    }


    @Override
    public Link getLink()
    {
        return driveTask.getArc().getFromLink();
    }


    @Override
    public int getLinkEnterTime()
    {
        return driveTask.getBeginTime();
    }

    @Override
    public int predictLinkExitTime(int currentTime)
    {
        return predictEndTime(currentTime);
    }


    @Override
    public int calculateCurrentDelay(int currentTime)
    {
        return Math.max(0, currentTime - initialEndTime);
    }


    @Override
    public int predictEndTime(int currentTime)
    {
        return Math.max(initialEndTime, currentTime);
    }


    @Override
    public int getInitialEndTime()
    {
        return initialEndTime;
    }
}
