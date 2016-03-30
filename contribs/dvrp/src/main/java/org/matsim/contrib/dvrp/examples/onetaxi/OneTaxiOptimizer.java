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

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


/**
 * @author michalm
 */
public class OneTaxiOptimizer
    implements VrpOptimizer
{
    private final MobsimTimer timer;

    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;

    private final Vehicle vehicle;//we have only one vehicle
    private final Schedule<AbstractTask> schedule;// the vehicle's schedule

    public static final double PICKUP_DURATION = 120;
    public static final double DROPOFF_DURATION = 60;


    @SuppressWarnings("unchecked")
    public OneTaxiOptimizer(Scenario scenario, VrpData vrpData, MobsimTimer timer)
    {
        this.timer = timer;

        travelTime = new FreeSpeedTravelTime();
        router = new Dijkstra(scenario.getNetwork(), new TimeAsTravelDisutility(travelTime),
                travelTime);

        vehicle = vrpData.getVehicles().values().iterator().next();
        vehicle.resetSchedule();//necessary if we run more than 1 iteration
        schedule = (Schedule<AbstractTask>)vehicle.getSchedule();
        schedule.addTask(
                new StayTaskImpl(vehicle.getT0(), vehicle.getT1(), vehicle.getStartLink(), "wait"));
    }


    @Override
    public void requestSubmitted(Request request)
    {
        StayTask lastTask = (StayTask)Schedules.getLastTask(schedule);// only WaitTask possible here
        double currentTime = timer.getTimeOfDay();

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

        double t0 = schedule.getStatus() == ScheduleStatus.UNPLANNED ? //
                Math.max(vehicle.getT0(), currentTime) : //
                Schedules.getLastTask(schedule).getEndTime();

        VrpPathWithTravelData p1 = VrpPaths.calcAndCreatePath(lastTask.getLink(), fromLink, t0,
                router, travelTime);
        schedule.addTask(new DriveTaskImpl(p1));

        double t1 = p1.getArrivalTime();
        double t2 = t1 + PICKUP_DURATION;// 2 minutes for picking up the passenger
        schedule.addTask(new OneTaxiServeTask(t1, t2, fromLink, true, req));

        VrpPathWithTravelData p2 = VrpPaths.calcAndCreatePath(fromLink, toLink, t2, router,
                travelTime);
        schedule.addTask(new DriveTaskImpl(p2));

        double t3 = p2.getArrivalTime();
        double t4 = t3 + DROPOFF_DURATION;// 1 minute for dropping off the passenger
        schedule.addTask(new OneTaxiServeTask(t3, t4, toLink, false, req));

        //just wait (and be ready) till the end of the vehicle's time window (T1)
        double tEnd = Math.max(t4, vehicle.getT1());
        schedule.addTask(new StayTaskImpl(t4, tEnd, toLink, "wait"));
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        shiftTimings();
        schedule.nextTask();
    }


    /**
     * Simplified version. For something more advanced, see
     * {@link org.matsim.contrib.taxi.scheduler.TaxiScheduler#updateBeforeNextTask(Schedule)} in the
     * taxi contrib
     */
    private void shiftTimings()
    {
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double now = timer.getTimeOfDay();
        Task currentTask = schedule.getCurrentTask();
        double diff = now - currentTask.getEndTime();

        if (diff == 0) {
            return;
        }

        currentTask.setEndTime(now);

        List<AbstractTask> tasks = schedule.getTasks();
        int nextTaskIdx = currentTask.getTaskIdx() + 1;

        //all except the last task (waiting)
        for (int i = nextTaskIdx; i < tasks.size() - 1; i++) {
            Task task = tasks.get(i);
            task.setBeginTime(task.getBeginTime() + diff);
            task.setEndTime(task.getEndTime() + diff);
        }

        //wait task
        if (nextTaskIdx != tasks.size()) {
            Task waitTask = tasks.get(tasks.size() - 1);
            waitTask.setBeginTime(waitTask.getBeginTime() + diff);

            double tEnd = Math.max(waitTask.getBeginTime(), vehicle.getT1());
            waitTask.setEndTime(tEnd);
        }
    }
}
