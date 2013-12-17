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

package org.matsim.contrib.dvrp.examples.dapp;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.DeliveryRequest;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.schedule.impl.DeliveryTaskImpl;


/**
 * @author michalm
 */
public class DAPPOptimizer
    implements VrpOptimizer
{
    private final VrpData data;
    private final Vehicle vehicle;//we have only one vehicle
    private final Schedule<AbstractTask> schedule;// the vehicle's schedule


    @SuppressWarnings("unchecked")
    public DAPPOptimizer(VrpData data)
    {
        this.data = data;
        vehicle = data.getVehicles().get(0);
        schedule = (Schedule<AbstractTask>)vehicle.getSchedule();
    }


    /**
     */
    @Override
    public void init()
    {
        //just wait (and be ready) till the end of the vehicle's time window (T1)
        schedule.addTask(new StayTaskImpl(vehicle.getT0(), Schedules.getActualT1(schedule), vehicle
                .getDepot().getVertex()));
    }


    @Override
    public void requestSubmitted(Request request)
    {
        StayTask lastTask = (StayTask)Schedules.getLastTask(schedule);// only waiting
        int currentTime = data.getTime();

        switch (lastTask.getStatus()) {
            case PLANNED:
                schedule.removeLastTask();// remove waiting
                break;

            case STARTED:
                lastTask.setEndTime(currentTime);// shorten waiting
                break;

            default:
                throw new IllegalStateException();
        }

        DeliveryRequest req = (DeliveryRequest)request;
        int t0 = Schedules.getLastTask(schedule).getEndTime();

        Arc arc = data.getVrpGraph().getArc(lastTask.getVertex(), req.getToVertex());
        int t1 = t0 + arc.getTimeOnDeparture(t0);
        schedule.addTask(new DriveTaskImpl(t0, t1, arc));

        int t2 = t1 + 120;// 2 minutes for deliverying a pizza
        schedule.addTask(new DeliveryTaskImpl(t1, t2, req));

        //just wait (and be ready) till the end of the vehicle's time window (T1)
        int t3 = Schedules.getActualT1(schedule);
        schedule.addTask(new StayTaskImpl(t2, t3, req.getToVertex(), "wait"));
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        schedule.nextTask();
    }
}
