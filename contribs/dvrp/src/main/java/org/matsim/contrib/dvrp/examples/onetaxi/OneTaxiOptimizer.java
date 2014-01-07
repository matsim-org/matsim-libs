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
import org.matsim.contrib.dvrp.data.network.shortestpath.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;


/**
 * @author michalm
 */
public class OneTaxiOptimizer
    implements VrpOptimizer
{
    private final VrpData data;
    private final ShortestPathCalculator calculator;

    private final Vehicle vehicle;//we have only one vehicle
    private final Schedule<AbstractTask> schedule;// the vehicle's schedule


    @SuppressWarnings("unchecked")
    public OneTaxiOptimizer(VrpData data)
    {
        this.data = data;

        calculator = data.getShortestPathCalculator();
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
        StayTask lastTask = (StayTask)Schedules.getLastTask(schedule);// only WaitTask possible here
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

        ShortestPath sp1 = calculator.calculateShortestPath(lastTask.getLink(), fromLink, t0);
        schedule.addTask(new DriveTaskImpl(sp1));

        int t1 = t0 + sp1.travelTime;
        int t2 = t1 + 120;// 2 minutes for picking up the passenger
        schedule.addTask(new OneTaxiServeTask(t1, t2, fromLink, "pickup", req));

        ShortestPath sp2 = calculator.calculateShortestPath(fromLink, toLink, t2);
        schedule.addTask(new DriveTaskImpl(sp2));

        int t3 = t2 + sp2.travelTime;
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
