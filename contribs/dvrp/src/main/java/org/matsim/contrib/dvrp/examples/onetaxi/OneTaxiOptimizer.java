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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;


/**
 * @author michalm
 */
public class OneTaxiOptimizer
    implements VrpOptimizer
{
    private final VrpData data;
    private final Vehicle vehicle;//we have only one vehicle
    private final Schedule<AbstractTask> schedule;// the vehicle's schedule


    @SuppressWarnings("unchecked")
    public OneTaxiOptimizer(VrpData data)
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
                .getDepot().getLink()));
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

        OneTaxiRequest req = (OneTaxiRequest)request;
        Link fromLink = req.getFromLink();
        Link toLink = req.getToLink();
        int t0 = Schedules.getLastTask(schedule).getEndTime();

        Arc pickupArc = data.getVrpGraph().getArc(lastTask.getLink(), fromLink);
        int t1 = t0 + pickupArc.getShortestPath(t0).travelTime;
        schedule.addTask(new DriveTaskImpl(t0, t1, pickupArc));

        int t2 = t1 + 120;// 2 minutes for picking up the passenger
        schedule.addTask(new OneTaxiServeTask(t1, t2, fromLink, "pickup", req));

        Arc deliveryArc = data.getVrpGraph().getArc(fromLink, toLink);
        int t3 = t2 + deliveryArc.getShortestPath(t2).travelTime;
        schedule.addTask(new DriveTaskImpl(t2, t3, deliveryArc));

        int t4 = t3 + 60;// 1 minute for dropping off the passenger
        schedule.addTask(new OneTaxiServeTask(t3, t4, toLink, "dropoff", req));

        //just wait (and be ready) till the end of the vehicle's time window (T1)
        int tEnd = Schedules.getActualT1(schedule);
        schedule.addTask(new StayTaskImpl(t4, tEnd, toLink, "wait"));
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        schedule.nextTask();
    }
}
