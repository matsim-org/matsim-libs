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

import org.matsim.contrib.dvrp.schedule.Task;


public class OfflineTaskTracker
    implements TaskTracker
{
    private final Task task;
    private final double plannedEndTime;


    public OfflineTaskTracker(Task task)
    {
        this.task = task;
        this.plannedEndTime = task.getEndTime();
    }


    @Override
    public double getBeginTime()
    {
        return task.getBeginTime();
    }


    @Override
    public double getPlannedEndTime()
    {
        return plannedEndTime;
    }


    @Override
    public double predictEndTime(double currentTime)
    {
        return Math.max(plannedEndTime, currentTime);
    }

}
