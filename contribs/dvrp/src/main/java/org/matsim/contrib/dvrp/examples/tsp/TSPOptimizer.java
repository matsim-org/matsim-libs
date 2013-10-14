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

package org.matsim.contrib.dvrp.examples.tsp;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;


/**
 * @author michalm
 */
public class TSPOptimizer
    implements VrpOptimizer
{
    private final VrpData data;
    private final Vehicle vehicle;//we have only one vehicle
    private final Schedule<TaskImpl> schedule;


    @SuppressWarnings("unchecked")
    public TSPOptimizer(VrpData data)
    {
        this.data = data;
        vehicle = data.getVehicles().get(0);
        schedule = (Schedule<TaskImpl>)vehicle.getSchedule();
    }


    /**
     */
    @Override
    public void init()
    {
        schedule.addTask(new StayTaskImpl(vehicle.getT0(), vehicle.getT1(), vehicle.getDepot()
                .getVertex()));
    }


    @Override
    public void requestSubmitted(Request request)
    {
        StayTask lastTask = (StayTask)Schedules.getLastTask(schedule);// only waiting
        int currentTime = data.getTime();

        switch (lastTask.getStatus()) {
            case PLANNED:
                schedule.removeLastPlannedTask();// remove waiting
                break;

            case STARTED:
                lastTask.setEndTime(currentTime);// shortening waiting
                break;

            case PERFORMED:
            default:
                throw new IllegalStateException();
        }

        int t0;
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            t0 = currentTime;
        }
        else {
            t0 = Schedules.getLastTask(schedule).getEndTime();
        }

        Arc arc = data.getVrpGraph().getArc(request.getFromVertex(), request.getToVertex());
        schedule.addTask(new DriveTaskImpl(t0, arc.getTimeOnDeparture(t0), arc));

    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        schedule.nextTask();
    }
}
