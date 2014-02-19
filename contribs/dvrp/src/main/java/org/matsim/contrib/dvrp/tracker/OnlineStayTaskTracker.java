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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynActivity;


public class OnlineStayTaskTracker
    implements TaskTracker
{
    private final StayTask stayTask;
    private final DynActivity dynActivity;
    private final double plannedEndTime;


    public OnlineStayTaskTracker(StayTask stayTask, DynActivity dynActivity)
    {
        this.stayTask = stayTask;
        this.dynActivity = dynActivity;
        this.plannedEndTime = stayTask.getEndTime();
    }


    @Override
    public double getBeginTime()
    {
        return stayTask.getBeginTime();
    }


    @Override
    public double predictEndTime(double currentTime)
    {
        return dynActivity.getEndTime();
    }


    @Override
    public double getPlannedEndTime()
    {
        return plannedEndTime;
    }
}
