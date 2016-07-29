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

package org.matsim.contrib.taxi.scheduler;

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.misc.Time;


public class TaxiScheduler
    implements TaxiScheduleInquiry
{
    private final TaxiData taxiData;
    protected final TaxiSchedulerParams params;
    private final MobsimTimer timer;

    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;


    public TaxiScheduler(Scenario scenario, TaxiData taxiData, MobsimTimer timer,
            TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility)
    {
        this.taxiData = taxiData;
        this.params = params;
        this.timer = timer;
        this.travelTime = travelTime;

        PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
        preProcessEuclidean.run(scenario.getNetwork());

        FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
        RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
                .createRoutingNetwork(scenario.getNetwork());

        router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility,
                travelTime, params.AStarEuclideanOverdoFactor, fastRouterFactory);

        taxiData.clearRequestsAndResetSchedules();

        for (Vehicle veh : taxiData.getVehicles().values()) {
            TaxiSchedules.asTaxiSchedule(veh.getSchedule())
                    .addTask(new TaxiStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public TaxiSchedulerParams getParams()
    {
        return params;
    }


    @Override
    public boolean isIdle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        if (timer.getTimeOfDay() >= vehicle.getT1()
                || schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxiTask currentTask = schedule.getCurrentTask();
        return Schedules.isLastTask(currentTask)
                && currentTask.getTaxiTaskType() == TaxiTaskType.STAY;
    }


    /**
     * If the returned LinkTimePair is not null, then time is not smaller than the current time
     */
    @Override
    public LinkTimePair getImmediateDiversionOrEarliestIdleness(Vehicle veh)
    {
        if (params.vehicleDiversion) {
            LinkTimePair diversion = getImmediateDiversion(veh);
            if (diversion != null) {
                return diversion;
            }
        }

        return getEarliestIdleness(veh);
    }


    /**
     * If the returned LinkTimePair is not null, then time is not smaller than the current time
     */
    @Override
    public LinkTimePair getEarliestIdleness(Vehicle veh)
    {
        if (timer.getTimeOfDay() >= veh.getT1()) {// time window T1 exceeded
            return null;
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(veh.getSchedule());
        Link link;
        double time;

        switch (schedule.getStatus()) {
            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxiTaskType()) {
                    case STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());//TODO very optimistic!!!
                        return createValidLinkTimePair(link, time, veh);

                    case PICKUP:
                        if (!params.destinationKnown) {
                            return null;
                        }
                        //otherwise: IllegalStateException -- the schedule should end with STAY (or PICKUP if unfinished)

                    default:
                        throw new IllegalStateException(
                                "Type of the last task is wrong: " + lastTask.getTaxiTaskType());
                }

            case COMPLETED:
                return null;

            case UNPLANNED://there is always at least one WAIT task in a schedule
            default:
                throw new IllegalStateException();
        }
    }


    /**
     * If the returned LinkTimePair is not null, then time is not smaller than the current time
     */
    @Override
    public LinkTimePair getImmediateDiversion(Vehicle veh)
    {
        if (!params.vehicleDiversion) {
            throw new RuntimeException("Diversion must be on");
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(veh.getSchedule());
        if (/*context.getTime() >= veh.getT1() ||*/schedule.getStatus() != ScheduleStatus.STARTED) {
            return null;
        }

        TaxiTask currentTask = schedule.getCurrentTask();
        //we can divert vehicle whose current task is an empty drive at the end of the schedule
        if (!Schedules.isLastTask(currentTask)
                || currentTask.getTaxiTaskType() != TaxiTaskType.EMPTY_DRIVE) {
            return null;
        }

        OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)currentTask.getTaskTracker();
        return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
    }


    private LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh)
    {
        return pair.time >= veh.getT1() ? null : pair;
    }


    private LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh)
    {
        return time >= veh.getT1() ? null : new LinkTimePair(link, time);
    }


    //=========================================================================================

    public void scheduleRequest(Vehicle vehicle, TaxiRequest request, VrpPathWithTravelData vrpPath)
    {
        if (request.getStatus() != TaxiRequestStatus.UNPLANNED) {
            throw new IllegalStateException();
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        divertOrAppendDrive(schedule, vrpPath);

        double pickupEndTime = Math.max(vrpPath.getArrivalTime(), request.getT0())
                + params.pickupDuration;
        schedule.addTask(new TaxiPickupTask(vrpPath.getArrivalTime(), pickupEndTime, request));

        if (params.destinationKnown) {
            appendOccupiedDriveAndDropoff(schedule);
            appendTasksAfterDropoff(schedule);
        }
    }


    protected void divertOrAppendDrive(Schedule<TaxiTask> schedule, VrpPathWithTravelData vrpPath)
    {
        TaxiTask lastTask = Schedules.getLastTask(schedule);
        switch (lastTask.getTaxiTaskType()) {
            case EMPTY_DRIVE:
                divertDrive((TaxiEmptyDriveTask)lastTask, vrpPath);
                return;

            case STAY:
                scheduleDrive((TaxiStayTask)lastTask, vrpPath);
                return;

            default:
                throw new IllegalStateException();
        }
    }


    protected void divertDrive(TaxiEmptyDriveTask lastTask, VrpPathWithTravelData vrpPath)
    {
        if (!params.vehicleDiversion) {
            throw new IllegalStateException();
        }

        ((OnlineDriveTaskTracker)lastTask.getTaskTracker()).divertPath(vrpPath);
    }


    protected void scheduleDrive(TaxiStayTask lastTask, VrpPathWithTravelData vrpPath)
    {
        Schedule<TaxiTask> bestSched = TaxiSchedules.asTaxiSchedule(lastTask.getSchedule());

        switch (lastTask.getStatus()) {
            case PLANNED:
                if (lastTask.getBeginTime() == vrpPath.getDepartureTime()) { // waiting for 0 seconds!!!
                    bestSched.removeLastTask();// remove WaitTask
                }
                else {
                    // actually this WAIT task will not be performed
                    lastTask.setEndTime(vrpPath.getDepartureTime());// shortening the WAIT task
                }
                break;

            case STARTED:
                lastTask.setEndTime(vrpPath.getDepartureTime());// shortening the WAIT task
                break;

            case PERFORMED:
            default:
                throw new IllegalStateException();
        }

        if (vrpPath.getLinkCount() > 1) {
            bestSched.addTask(new TaxiEmptyDriveTask(vrpPath));
        }
    }


    /**
     * If diversion is enabled, this method must be called after scheduling in order to make sure
     * that no vehicle is moving aimlessly.
     * <p/>
     * The reason: the destination/goal had been removed before scheduling (e.g. by calling the
     * {@link #removeAwaitingRequestsFromAllSchedules()} method)
     */
    public void stopAllAimlessDriveTasks()
    {
        for (Vehicle veh : taxiData.getVehicles().values()) {
            if (getImmediateDiversion(veh) != null) {
                stopVehicle(veh);
            }
        }
    }


    public void stopVehicle(Vehicle veh)
    {
        if (!params.vehicleDiversion) {
            throw new RuntimeException("Diversion must be on");
        }

        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(veh.getSchedule());
        TaxiEmptyDriveTask driveTask = (TaxiEmptyDriveTask)Schedules.getLastTask(schedule);

        OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)driveTask.getTaskTracker();
        LinkTimePair stopPoint = tracker.getDiversionPoint();
        tracker.divertPath(new VrpPathWithTravelDataImpl(stopPoint.time, 0,
                new Link[] { stopPoint.link }, new double[] { 0 }));

        appendStayTask(schedule);
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

        double endTime = timer.getTimeOfDay();
        TaxiTask currentTask = schedule.getCurrentTask();

        updateTimelineImpl(schedule, endTime);

        if (!params.destinationKnown) {
            if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                appendOccupiedDriveAndDropoff(schedule);
                appendTasksAfterDropoff(schedule);
            }
        }
    }


    protected void appendOccupiedDriveAndDropoff(Schedule<TaxiTask> schedule)
    {
        TaxiPickupTask pickupStayTask = (TaxiPickupTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        TaxiRequest req = ((TaxiPickupTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new TaxiOccupiedDriveTask(path, req));

        double t4 = path.getArrivalTime();
        double t5 = t4 + params.dropoffDuration;
        schedule.addTask(new TaxiDropoffTask(t4, t5, req));
    }


    protected VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime)
    {
        return VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime);
    }


    protected void appendTasksAfterDropoff(Schedule<TaxiTask> schedule)
    {
        appendStayTask(schedule);
    }


    protected void appendStayTask(Schedule<TaxiTask> schedule)
    {
        double tBegin = schedule.getEndTime();
        double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());//even 0-second WAIT
        Link link = Schedules.getLastLinkInSchedule(schedule);
        schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
    }


    public void updateTimeline(Schedule<TaxiTask> schedule)
    {
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(),
                timer.getTimeOfDay());
        updateTimelineImpl(schedule, predictedEndTime);
    }


    private void updateTimelineImpl(Schedule<TaxiTask> schedule, double newEndTime)
    {
        Task currentTask = schedule.getCurrentTask();
        if (currentTask.getEndTime() == newEndTime) {
            return;
        }

        currentTask.setEndTime(newEndTime);

        List<TaxiTask> tasks = schedule.getTasks();
        int startIdx = currentTask.getTaskIdx() + 1;
        double newBeginTime = newEndTime;

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxiTask task = tasks.get(i);
            double calcEndTime = calcNewEndTime(task, newBeginTime);

            if (calcEndTime == Time.UNDEFINED_TIME) {
                schedule.removeTask(task);
                i--;
            }
            else if (calcEndTime < newBeginTime) {//0 s is fine (e.g. last 'wait')
                throw new IllegalStateException();
            }
            else {
                task.setBeginTime(newBeginTime);
                task.setEndTime(calcEndTime);
                newBeginTime = calcEndTime;
            }
        }
    }


    protected double calcNewEndTime(TaxiTask task, double newBeginTime)
    {
        switch (task.getTaxiTaskType()) {
            case STAY: {
                if (Schedules.isLastTask(task)) {// last task
                    //even if endTime=beginTime, do not remove this task!!! A taxi schedule should end with WAIT 
                    return Math.max(newBeginTime, task.getSchedule().getVehicle().getT1());
                }
                else {
                    // if this is not the last task then some other task (e.g. DRIVE or PICKUP)
                    // must have been added at time submissionTime <= t
                    double oldEndTime = task.getEndTime();
                    if (oldEndTime <= newBeginTime) {// may happen if the previous task is delayed
                        return Time.UNDEFINED_TIME;//remove the task
                    }
                    else {
                        return oldEndTime;
                    }
                }
            }

            case EMPTY_DRIVE:
            case OCCUPIED_DRIVE: {
                // cannot be shortened/lengthen, therefore must be moved forward/backward
                VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask)task).getPath();
                //TODO one may consider recalculation of SP!!!!
                return newBeginTime + path.getTravelTime();
            }

            case PICKUP: {
                double t0 = ((TaxiPickupTask)task).getRequest().getT0();// t0 == passenger's departure time
                // the actual pickup starts at max(t, t0)
                return Math.max(newBeginTime, t0) + params.pickupDuration;
            }
            case DROPOFF: {
                // cannot be shortened/lengthen, therefore must be moved forward/backward
                return newBeginTime + params.dropoffDuration;
            }

            default:
                throw new IllegalStateException();
        }
    }


    //=========================================================================================

    private List<TaxiRequest> removedRequests;


    /**
     * Awaiting == unpicked-up, i.e. requests with status PLANNED or TAXI_DISPATCHED See
     * {@link TaxiRequestStatus}
     */
    public List<TaxiRequest> removeAwaitingRequestsFromAllSchedules()
    {
        removedRequests = new ArrayList<>();
        for (Vehicle veh : taxiData.getVehicles().values()) {
            removeAwaitingRequestsImpl(TaxiSchedules.asTaxiSchedule(veh.getSchedule()));
        }

        return removedRequests;
    }


    public List<TaxiRequest> removeAwaitingRequests(Schedule<TaxiTask> schedule)
    {
        removedRequests = new ArrayList<>();
        removeAwaitingRequestsImpl(schedule);
        return removedRequests;
    }


    private void removeAwaitingRequestsImpl(Schedule<TaxiTask> schedule)
    {
        switch (schedule.getStatus()) {
            case STARTED:
                Integer unremovableTasksCount = countUnremovablePlannedTasks(schedule);
                if (unremovableTasksCount == null) {
                    return;
                }

                int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + unremovableTasksCount;
                removePlannedTasks(schedule, newLastTaskIdx);
                cleanupScheduleAfterTaskRemoval(schedule);
                return;

            case PLANNED:
                removePlannedTasks(schedule, -1);
                cleanupScheduleAfterTaskRemoval(schedule);
                return;

            case COMPLETED:
                return;

            case UNPLANNED:
                throw new IllegalStateException();
        }
    }


    protected Integer countUnremovablePlannedTasks(Schedule<TaxiTask> schedule)
    {
        TaxiTask currentTask = schedule.getCurrentTask();
        switch (currentTask.getTaxiTaskType()) {
            case PICKUP:
                return params.destinationKnown ? 2 : null;

            case OCCUPIED_DRIVE:
                return 1;

            case EMPTY_DRIVE:
                if (params.vehicleDiversion) {
                    return 0;
                }

                if (TaxiSchedules.getNextTaxiTask(currentTask)
                        .getTaxiTaskType() == TaxiTaskType.PICKUP) {
                    //if no diversion and driving to pick up sb then serve that request
                    return params.destinationKnown ? 3 : null;
                }

                //potentially: driving back to the rank (e.g. to charge batteries)
                throw new RuntimeException("Currently won't happen");

            case DROPOFF:
            case STAY:
                return 0;

            default:
                throw new RuntimeException();
        }
    }


    protected void removePlannedTasks(Schedule<TaxiTask> schedule, int newLastTaskIdx)
    {
        List<TaxiTask> tasks = schedule.getTasks();

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxiTask task = tasks.get(i);
            schedule.removeTask(task);
            taskRemovedFromSchedule(schedule, task);
        }
    }


    protected void taskRemovedFromSchedule(Schedule<TaxiTask> schedule, TaxiTask task)
    {
        if (task instanceof TaxiTaskWithRequest) {
            TaxiTaskWithRequest taskWithReq = (TaxiTaskWithRequest)task;
            taskWithReq.disconnectFromRequest();

            if (task.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                removedRequests.add(taskWithReq.getRequest());
            }
        }
    }
    
    
    //only for planned/started schedule
    private void cleanupScheduleAfterTaskRemoval(Schedule<TaxiTask> schedule)
    {
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            Vehicle veh = schedule.getVehicle();
            schedule.addTask(new TaxiStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
            return;
        }
        //else: PLANNED, STARTED
        
        TaxiTask lastTask = Schedules.getLastTask(schedule);
        double tBegin = schedule.getEndTime();
        double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());

        switch (lastTask.getTaxiTaskType()) {
            case STAY:
                lastTask.setEndTime(tEnd);
                return;

            case DROPOFF:
                Link link = Schedules.getLastLinkInSchedule(schedule);
                schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
                return;

            case EMPTY_DRIVE:
                if (!params.vehicleDiversion) {
                    throw new RuntimeException("Currently won't happen");
                }

                //if diversion -- no STAY afterwards
                return;

            default:
                throw new RuntimeException();
        }
    }
}
