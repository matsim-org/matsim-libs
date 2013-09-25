/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp;

import org.matsim.contrib.dvrp.dynagent.DynActivity;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


class TaxiTaskActivity
    implements DynActivity
{
    private StayTask stayTask;
    private String activityType;


    TaxiTaskActivity(String activityType, StayTask stayTask)
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


    static TaxiTaskActivity createServeActivity(ServeTask serveTask)
    {
        return new TaxiTaskActivity("ServeTask" + serveTask.getRequest().getId(), serveTask);
    }


    static TaxiTaskActivity createWaitActivity(WaitTask waitTask)
    {
        return new TaxiTaskActivity("WaitTask", waitTask);
    }
}
