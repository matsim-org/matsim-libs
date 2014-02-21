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

package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.TaxiDelaySpeedupStats;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiScheduler
{
    private final MatsimVrpContext context;

    private final VrpPathCalculator calculator;
    private final Comparator<VrpPathWithTravelData> pathComparator;

    private final ImmediateRequestParams params;

    private TaxiDelaySpeedupStats delaySpeedupStats;


    public TaxiScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params)
    {
        this.context = context;
        this.calculator = calculator;
        this.params = params;

        if (params.minimizePickupTripTime != null) {
            pathComparator = params.minimizePickupTripTime ? //
                    VrpPathWithTravelDataComparators.TRAVEL_TIME_COMPARATOR : //
                    VrpPathWithTravelDataComparators.ARRIVAL_TIME_COMPARATOR;
        }
        else {
            pathComparator = null;
        }

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
            schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    public ImmediateRequestParams getParams()
    {
        return params;
    }


    public VrpPathCalculator getCalculator()
    {
        return calculator;
    }


    public VehicleRequestPath findBestVehicleRequestPath(TaxiRequest req,
            Collection<Vehicle> vehicles)
    {
        VehicleRequestPath best = null;

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            VrpPathWithTravelData current = calculateVrpPath(veh, req);

            if (current == null) {
                continue;
            }
            else if (best == null) {
                best = new VehicleRequestPath(veh, req, current);
            }
            else if (pathComparator.compare(current, best.path) < 0) {
                // TODO: in the future: add a check if the taxi time windows are satisfied
                best = new VehicleRequestPath(veh, req, current);
            }
        }

        return best;
    }


    protected VehicleRequestPath findBestVehicleRequestPath(Vehicle veh,
            Collection<TaxiRequest> unplannedRequests)
    {
        VehicleRequestPath best = null;

        for (TaxiRequest req : unplannedRequests) {
            VrpPathWithTravelData current = calculateVrpPath(veh, req);

            if (current == null) {
                continue;
            }
            else if (best == null) {
                best = new VehicleRequestPath(veh, req, current);
            }
            else if (pathComparator.compare(current, best.path) < 0) {
                // TODO: in the future: add a check if the taxi time windows are satisfied
                best = new VehicleRequestPath(veh, req, current);
            }
        }

        return best;
    }


    public VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxiRequest req)
    {
        LinkTimePair departure = getEarliestIdleness(veh);
        return departure == null ? null : calculator.calcPath(departure.link, req.getFromLink(),
                departure.time);
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
                        return new LinkTimePair(link, time);

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
                return new LinkTimePair(link, time);

            case STARTED:
                TaxiTask currentTask = schedule.getCurrentTask();

                switch (currentTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)currentTask).getLink();
                        time = currentTime;
                        return new LinkTimePair(link, time);

                    case PICKUP_DRIVE:
                        TaskTracker tracker = currentTask.getTaskTracker();

                        if (tracker instanceof OnlineDriveTaskTracker) {
                            return ((OnlineDriveTaskTracker)tracker).getDiversionPoint(currentTime);
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
                return new LinkTimePair(link, time);

            case COMPLETED:
                return null;

            case UNPLANNED://there is always at least one WAIT task in a schedule
            default:
                throw new IllegalStateException();
        }
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

        double t3 = Math.max(best.path.getArrivalTime(), best.request.getT0()) + params.pickupDuration;
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

                        if (task.getEndTime() < t) {// may happen if a previous task was delayed
                            // I used to remove this WAIT_TASK, but now I keep it in the schedule:
                            // schedule.removePlannedTask(task.getTaskIdx());
                            task.setEndTime(t);
                        }
                    }
                    else {
                        // if this is not the last task then some other task must have been added
                        // at time t0 <= t
                        // THEREFORE: waittask.endTime() == t0, and so it can be removed

                        TaxiTask nextTask = tasks.get(i + 1);
                        switch (nextTask.getTaxiTaskType()) {
                            case PICKUP_DRIVE:

                                TaxiRequest req = ((TaxiPickupDriveTask)nextTask).getRequest();

                                if (req.getT0() > req.getSubmissionTime()) {//advance requests
                                    //currently no support
                                    throw new RuntimeException();
                                }
                                else {//immediate requests
                                    schedule.removeTask(task);
                                    i--;
                                }

                                break;

                            default:
                                //maybe in the future: WAIT+CHARGE or WAIT+CRUISE would make sense
                                //but currently it is not supported
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


    private interface RequestAdder
    {
        void addRequest(TaxiRequest request);
    };


    public void removePlannedRequests(Schedule<TaxiTask> schedule,
            final Collection<TaxiRequest> collection)
    {
        removePlannedRequestsImpl(schedule, new RequestAdder() {
            public void addRequest(TaxiRequest request)
            {
                collection.add(request);
            }
        });
    }


    public void removePlannedRequests(Schedule<TaxiTask> schedule, final Map<Id, TaxiRequest> map)
    {
        removePlannedRequestsImpl(schedule, new RequestAdder() {
            public void addRequest(TaxiRequest request)
            {
                map.put(request.getId(), request);
            }
        });
    }


    private void removePlannedRequestsImpl(Schedule<TaxiTask> schedule,
            RequestAdder unplannedRequestAdder)
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
                        obligatoryTasks = 0;

                    case WAIT_STAY:
                        // this WAIT is not the last task
                        // but one cannot wait if there are more tasks scheduled 
                        throw new IllegalStateException();
                }

                if (obligatoryTasks == 0) {
                    return;
                }

                removePlannedTasks(schedule, obligatoryTasks, unplannedRequestAdder);

                double tBegin = schedule.getEndTime();
                double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());
                Link link = Schedules.getLastLinkInSchedule(schedule);
                schedule.addTask(new TaxiWaitStayTask(tBegin, tEnd, link));

                break;

            case UNPLANNED:
            case COMPLETED:
                break;

            case PLANNED:// at time 0, taxi agents should start WAIT (before first taxi call)
                // therefore PLANNED->STARTED happens at the very beginning of time step 0
            default:
                throw new IllegalStateException();
        }
    }


    private void removePlannedTasks(Schedule<TaxiTask> schedule, int obligatoryTasks,
            RequestAdder unplannedRequestAdder)
    {
        List<TaxiTask> tasks = schedule.getTasks();
        int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + obligatoryTasks;

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxiTask task = tasks.get(i);

            schedule.removeTask(task);

            if (task instanceof TaxiTaskWithRequest) {
                TaxiTaskWithRequest taskWithReq = (TaxiTaskWithRequest)task;
                taskWithReq.removeFromRequest();

                if (task.getTaxiTaskType() == TaxiTaskType.PICKUP_DRIVE) {
                    unplannedRequestAdder.addRequest(taskWithReq.getRequest());
                }
            }
        }
    }
}
