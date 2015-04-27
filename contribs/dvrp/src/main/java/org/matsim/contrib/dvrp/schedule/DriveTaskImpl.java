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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.router.*;


public class DriveTaskImpl
    extends AbstractTask
    implements DriveTask
{
    private VrpPath path;


    public DriveTaskImpl(VrpPathWithTravelData path)
    {
        super(path.getDepartureTime(), path.getArrivalTime());
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
    public void pathDiverted(DivertedVrpPath divertedPath, double newEndTime)
    {
        //can only divert an ongoing task
        if (getStatus() != TaskStatus.STARTED) {
            throw new IllegalStateException();
        }

        path = divertedPath;
        setEndTime(newEndTime);
    }


    @Override
    public String toString()
    {
        return "D(@" + path.getFromLink().getId() + "->@" + path.getToLink().getId() + ")"
                + commonToString();
    }
}