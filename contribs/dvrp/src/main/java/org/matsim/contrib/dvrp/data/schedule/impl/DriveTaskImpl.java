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

package org.matsim.contrib.dvrp.data.schedule.impl;

import org.matsim.contrib.dvrp.data.schedule.DriveTask;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.tracker.OfflineVehicleTracker;


public class DriveTaskImpl
    extends AbstractTask
    implements DriveTask
{
    private VrpPath path;
    private OfflineVehicleTracker vehicleTracker;


    public DriveTaskImpl(VrpPathWithTravelData path)
    {
        this(path, path.getDepartureTime(), path.getArrivalTime());
    }


    public DriveTaskImpl(VrpPath path, int beginTime, int endTime)
    {
        super(beginTime, endTime);
        this.path = path;
    }


    @Override
    public TaskType getType()
    {
        return TaskType.DRIVE;
    }


    @Override
    public VrpPath getPath()
    {
        return path;
    }


    @Override
    public void pathDiverted(DivertedVrpPath divertedPath, int newEndTime)
    {
        //can only divert an ongoing task
        if (getStatus() != TaskStatus.STARTED) {
            throw new IllegalStateException();
        }

        //divertedPath must be derived from the original one 
        if (divertedPath.getOriginalPath() != path) {
            throw new IllegalArgumentException();
        }

        path = divertedPath;
        setEndTime(newEndTime);
    }


    @Override
    public OfflineVehicleTracker getVehicleTracker()
    {
        return vehicleTracker;
    }


    @Override
    public void setVehicleTracker(OfflineVehicleTracker vehicleTracker)
    {
        this.vehicleTracker = vehicleTracker;
    }


    @Override
    public String toString()
    {
        return "D(@" + path.getFromLink().getId() + "->@" + path.getToLink().getId() + ")"
                + commonToString();
    }
}