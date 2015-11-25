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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynActivity;


public class VrpActivity
    implements DynActivity
{
    private final StayTask stayTask;
    private final String activityType;


    public VrpActivity(String activityType, StayTask stayTask)
    {
        this.activityType = activityType;
        this.stayTask = stayTask;
    }


    @Override
    public double getEndTime()
    {
        return stayTask.getEndTime();
    }


    @Override
    public String getActivityType()
    {
        return activityType;
    }


    @Override
    public void doSimStep(double now)
    {}


    @Override
    public void finalizeAction(double now)
    {}
}
