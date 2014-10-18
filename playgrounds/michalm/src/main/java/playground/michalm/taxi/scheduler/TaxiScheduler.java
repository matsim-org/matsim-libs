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

package playground.michalm.taxi.scheduler;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class TaxiScheduler
{
    private final MatsimVrpContext context;
    private final VrpPathCalculator calculator;
    private final TaxiSchedulerParams params;

    private TaxiDelaySpeedupStats delaySpeedupStats;


    public TaxiScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiSchedulerParams params)
    {
        this.context = context;
        this.calculator = calculator;
        this.params = params;

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
            schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public TaxiSchedulerParams getParams()
    {
        return params;
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    public boolean isIdle(Vehicle vehicle)
    {
        double currentTime = context.getTime();
        if (currentTime >= vehicle.getT1()) {// time window T1 exceeded
            return false;
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(vehicle);
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxiTask currentTask = schedule.getCurrentTask();

        return Schedules.isLastTask(currentTask)
                && currentTask.getTaxiTaskType() == TaxiTaskType.WAIT_STAY;
    }


    public LinkTimePair getEarliestIdleness(Vehicle veh)
    {
        double currentTime = context.getTime();
        if (currentTime >= veh.getT1()) {// time window T1 exceeded
            return null;
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
        Link link;
        double time;

        switch (schedule.getStatus()) {
            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), currentTime);//TODO very optimistic!!!
                        return createValidLinkTimePair(link, time, veh);

                    case PICKUP_STAY:
                        if (!params.destinationKnown) {
                            return null;
                        }
                        else {
                            throw new IllegalStateException();
                        }

                    default:
                        throw new IllegalStateException();
                }

            case COMPLETED:
                return null;

            case UNPLANNED://there is always at least one WAIT task in a schedule
            default:
                throw new IllegalStateException();
        }
    }


    public LinkTimePair getEarliestDiversion(Vehicle veh)
    {
        double currentTime = context.getTime();
        if (currentTime >= veh.getT1()) {// time window T1 exceeded
            return null;
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
        Link link;
        double time;
        TaxiDropoffStayTask dropoffStayTask;

        switch (schedule.getStatus()) {
            case PLANNED:
                link = veh.getStartLink();
                time = Math.max(veh.getT0(), currentTime);
                return createValidLinkTimePair(link, time, veh);

            case STARTED:
                TaxiTask currentTask = schedule.getCurrentTask();

                switch (currentTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)currentTask).getLink();
                        time = currentTime;
                        return createValidLinkTimePair(link, time, veh);

                    case PICKUP_DRIVE:
                        TaskTracker tracker = currentTask.getTaskTracker();

                        if (tracker instanceof OnlineDriveTaskTracker) {
                            OnlineDriveTaskTracker onlineTracker = (OnlineDriveTaskTracker)tracker;
                            LinkTimePair lt = onlineTracker.getDiversionPoint(currentTime);
                            return filterValidLinkTimePair(lt, veh);
                        }

                        //no "break" here!!!

                    case PICKUP_STAY:
                        if (!params.destinationKnown) {
                            return null;
                        }

                        //no "break" here!!!

                    case DROPOFF_DRIVE:
                        dropoffStayTask = ((TaxiTaskWithRequest)currentTask).getRequest()
                                .getDropoffStayTask();
                        break;

                    case DROPOFF_STAY:
                        dropoffStayTask = (TaxiDropoffStayTask)currentTask;
                        break;

                    default:
                        throw new IllegalStateException();
                }

                link = dropoffStayTask.getLink();
                time = dropoffStayTask.getEndTime();
                return createValidLinkTimePair(link, time, veh);

            case COMPLETED:
                return null;

            case UNPLANNED://there is always at least one WAIT task in a schedule
            default:
                throw new IllegalStateException();
        }
    }


    private LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh)
    {
        return pair.time >= veh.getT1() ? null : pair;
    }


    private LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh)
    {
        return time >= veh.getT1() ? null : new LinkTimePair(link, time);
    }


    public void scheduleRequest(VehicleRequestPath best)
    {
        if (best.request.getStatus() != TaxiRequestStatus.UNPLANNED) {
            throw new IllegalStateException();
        }

        Schedule<TaxiTask> bestSched = TaxiSchedules.getSchedule(best.vehicle);

        if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
            TaxiWaitStayTask lastTask = (TaxiWaitStayTask)Schedules.getLastTask(bestSched);// only WAIT

            switch (lastTask.getStatus()) {
                case PLANNED:
                    if (lastTask.getBeginTime() == best.path.getDepartureTime()) { // waiting for 0 seconds!!!
                        bestSched.removeLastTask();// remove WaitTask
                    }
                    else {
                        // actually this WAIT task will not be performed
                        lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task

                    }
                    break;

                case STARTED:
                    lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task
                    break;

                case PERFORMED:
                default:
                    throw new IllegalStateException();
            }
        }

        bestSched.addTask(new TaxiPickupDriveTask(best.path, best.request));

        double t3 = Math.max(best.path.getArrivalTime(), best.request.getT0())
                + params.pickupDuration;
        bestSched.addTask(new TaxiPickupStayTask(best.path.getArrivalTime(), t3, best.request));

        if (params.destinationKnown) {
            appendDropoffAfterPickup(bestSched);
            appendWaitAfterDropoff(bestSched);
        }
    }


    /**
     * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e.
     * beginTime and endTime) of all tasks in the schedule.
     */
    public void updateBeforeNextTask(Schedule<TaxiTask> schedule)
    {
        // Assumption: there is no delay as long as the schedule has not been started (PLANNED)
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double endTime = context.getTime();
        TaxiTask currentTask = schedule.getCurrentTask();

        if (delaySpeedupStats != null) {// optionally, one may record delays
            delaySpeedupStats.updateStats(currentTask, endTime);
        }

        updateCurrentAndPlannedTasks(schedule, endTime);

        if (!params.destinationKnown) {
            if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP_STAY) {
                appendDropoffAfterPickup(schedule);
                appendWaitAfterDropoff(schedule);
            }
        }
    }


    public void appendDropoffAfterPickup(Schedule<TaxiTask> schedule)
    {
        TaxiPickupStayTask pickupStayTask = (TaxiPickupStayTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        TaxiRequest req = ((TaxiPickupStayTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calculator.calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new TaxiDropoffDriveTask(path, req));

        double t4 = path.getArrivalTime();
        double t5 = t4 + params.dropoffDuration;
        schedule.addTask(new TaxiDropoffStayTask(t4, t5, req));
    }


    public void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
    {
        TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules.getLastTask(schedule);

        // addWaitTime at the end (even 0-second WAIT)
        double t5 = dropoffStayTask.getEndTime();
        double tEnd = Math.max(t5, schedule.getVehicle().getT1());
        Link link = dropoffStayTask.getLink();

        schedule.addTask(new TaxiWaitStayTask(t5, tEnd, link));
    }


    /**
     * @param schedule
     */
    public void updateCurrentAndPlannedTasks(Schedule<TaxiTask> schedule, double currentTaskEndTime)
    {
        Task currentTask = schedule.getCurrentTask();

        if (currentTask.getEndTime() == currentTaskEndTime) {
            return;
        }

        currentTask.setEndTime(currentTaskEndTime);

        List<TaxiTask> tasks = schedule.getTasks();

        int startIdx = currentTask.getTaskIdx() + 1;
        double t = currentTaskEndTime;

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxiTask task = tasks.get(i);

            switch (task.getTaxiTaskType()) {
                case WAIT_STAY: {
                    if (i == tasks.size() - 1) {// last task
                        task.setBeginTime(t);

                        if (task.getEndTime() < t) {// may happen if the previous task is delayed
                            task.setEndTime(t);//do not remove this task!!! A taxi schedule should end with WAIT
                        }
                    }
                    else {
                        // if this is not the last task then some other task must have been added
                        // at time submissionTime <= t

                        TaxiTask nextTask = tasks.get(i + 1);
                        switch (nextTask.getTaxiTaskType()) {
                            case PICKUP_DRIVE:
                            case CRUISE_DRIVE:
                                double endTime = task.getEndTime();

                                if (endTime <= t) {// may happen if the previous task is delayed
                                    schedule.removeTask(task);
                                    i--;
                                }
                                else {
                                    task.setBeginTime(t);
                                    t = endTime;
                                }

                                break;

                            default:
                                throw new RuntimeException();
                        }
                    }

                    break;
                }

                case PICKUP_DRIVE:
                case DROPOFF_DRIVE:
                case CRUISE_DRIVE: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask)task)
                            .getPath();
                    t += path.getTravelTime(); //TODO one may consider recalculation of SP!!!!
                    task.setEndTime(t);

                    break;
                }
                case PICKUP_STAY: {
                    task.setBeginTime(t);// t == taxi's arrival time
                    double t0 = ((TaxiPickupStayTask)task).getRequest().getT0();// t0 == passenger's departure time
                    t = Math.max(t, t0) + params.pickupDuration; // the true pickup starts at max(t, t0)
                    task.setEndTime(t);

                    break;
                }
                case DROPOFF_STAY: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += params.dropoffDuration;
                    task.setEndTime(t);

                    break;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }


    private List<TaxiRequest> removedRequests;


    public List<TaxiRequest> removePlannedRequestsFromAllSchedules()
    {
        removedRequests = new ArrayList<>();

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            removePlannedRequestsImpl(TaxiSchedules.getSchedule(veh));
        }

        return removedRequests;
    }


    public List<TaxiRequest> removePlannedRequests(Schedule<TaxiTask> schedule)
    {
        removedRequests = new ArrayList<>();
        removePlannedRequestsImpl(schedule);
        return removedRequests;
    }


    private void removePlannedRequestsImpl(Schedule<TaxiTask> schedule)
    {
        switch (schedule.getStatus()) {
            case STARTED:
                TaxiTask task = schedule.getCurrentTask();

                if (Schedules.isLastTask(task)) {
                    return;
                }

                int obligatoryTasks = 0;// remove all planned tasks

                switch (task.getTaxiTaskType()) {
                    case PICKUP_DRIVE:
                        if (!params.destinationKnown) {
                            return;
                        }

                        obligatoryTasks = 3;
                        break;

                    case PICKUP_STAY:
                        if (!params.destinationKnown) {
                            return;
                        }
                        obligatoryTasks = 2;
                        break;

                    case DROPOFF_DRIVE:
                        obligatoryTasks = 1;
                        break;

                    case DROPOFF_STAY:
                        obligatoryTasks = 0;
                        break;

                    case CRUISE_DRIVE:
                    case WAIT_STAY:
                        obligatoryTasks = 0;
                        break;
                }

                int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + obligatoryTasks;

                removePlannedTasks(schedule, newLastTaskIdx);

                double tBegin = schedule.getEndTime();
                double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());

                if (task.getTaxiTaskType() == TaxiTaskType.WAIT_STAY) {
                    task.setEndTime(tEnd);//extend WaitTask
                }
                else {
                    Link link = Schedules.getLastLinkInSchedule(schedule);
                    schedule.addTask(new TaxiWaitStayTask(tBegin, tEnd, link));
                }

                break;

            case PLANNED:
                removePlannedTasks(schedule, -1);
                Vehicle veh = schedule.getVehicle();
                schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));

                break;

            case COMPLETED:
                break;

            case UNPLANNED:
            default:
                throw new IllegalStateException();
        }
    }


    private void removePlannedTasks(Schedule<TaxiTask> schedule, int newLastTaskIdx)
    {
        List<TaxiTask> tasks = schedule.getTasks();

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxiTask task = tasks.get(i);

            schedule.removeTask(task);

            if (task instanceof TaxiTaskWithRequest) {
                TaxiTaskWithRequest taskWithReq = (TaxiTaskWithRequest)task;
                taskWithReq.removeFromRequest();

                if (task.getTaxiTaskType() == TaxiTaskType.PICKUP_DRIVE) {
                    removedRequests.add(taskWithReq.getRequest());
                }
            }
        }
    }
}
