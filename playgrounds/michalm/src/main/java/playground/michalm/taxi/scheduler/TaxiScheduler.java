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
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiScheduler
{
    protected final MatsimVrpContext context;
    protected final TaxiSchedulerParams params;

    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;


    public TaxiScheduler(MatsimVrpContext context, TaxiSchedulerParams params,
            TravelTime travelTime, TravelDisutility travelDisutility)
    {
        this.context = context;
        this.params = params;
        this.travelTime = travelTime;

        PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
        preProcessEuclidean.run(context.getScenario().getNetwork());

        FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
        RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
                .createRoutingNetwork(context.getScenario().getNetwork());

        router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility,
                travelTime, params.AStarEuclideanOverdoFactor, fastRouterFactory);

        for (Vehicle veh : context.getVrpData().getVehicles().values()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(veh.getSchedule());
            schedule.addTask(new TaxiStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public TaxiSchedulerParams getParams()
    {
        return params;
    }


    public boolean isIdle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        if (context.getTime() >= vehicle.getT1()
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
    public LinkTimePair getEarliestIdleness(Vehicle veh)
    {
        if (context.getTime() >= veh.getT1()) {// time window T1 exceeded
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
                        time = Math.max(lastTask.getBeginTime(), context.getTime());//TODO very optimistic!!!
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
        if (!Schedules.isLastTask(currentTask)
                || currentTask.getTaxiTaskType() != TaxiTaskType.DRIVE_EMPTY) {
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

        Schedule<TaxiTask> bestSched = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        TaxiTask lastTask = Schedules.getLastTask(bestSched);

        if (lastTask.getTaxiTaskType() == TaxiTaskType.DRIVE_EMPTY) {
            divertDriveToRequest((TaxiDriveTask)lastTask, vrpPath);
        }
        else if (lastTask.getTaxiTaskType() == TaxiTaskType.STAY) {
            scheduleDriveToRequest((TaxiStayTask)lastTask, vrpPath);
        }
        else {
            throw new IllegalStateException();
        }

        double t3 = Math.max(vrpPath.getArrivalTime(), request.getT0()) + params.pickupDuration;
        bestSched.addTask(new TaxiPickupTask(vrpPath.getArrivalTime(), t3, request));

        if (params.destinationKnown) {
            appendDriveAndDropoffAfterPickup(bestSched);
            appendTasksAfterDropoff(bestSched);
        }
    }


    private void divertDriveToRequest(TaxiDriveTask lastTask, VrpPathWithTravelData vrpPath)
    {
        if (!params.vehicleDiversion) {
            throw new IllegalStateException();
        }

        ((OnlineDriveTaskTracker)lastTask.getTaskTracker()).divertPath(vrpPath);
    }


    private void scheduleDriveToRequest(TaxiStayTask lastTask, VrpPathWithTravelData vrpPath)
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
            bestSched.addTask(new TaxiDriveTask(vrpPath));
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
        for (Vehicle veh : context.getVrpData().getVehicles().values()) {
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
        TaxiDriveTask driveTask = (TaxiDriveTask)Schedules.getLastTask(schedule);

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

        double endTime = context.getTime();
        TaxiTask currentTask = schedule.getCurrentTask();

        updateTimelineImpl(schedule, endTime);

        if (!params.destinationKnown) {
            if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                appendDriveAndDropoffAfterPickup(schedule);
                appendTasksAfterDropoff(schedule);
            }
        }
    }


    protected void appendDriveAndDropoffAfterPickup(Schedule<TaxiTask> schedule)
    {
        TaxiPickupTask pickupStayTask = (TaxiPickupTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        TaxiRequest req = ((TaxiPickupTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new TaxiDriveWithPassengerTask(path, req));

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
                context.getTime());
        updateTimelineImpl(schedule, predictedEndTime);
    }


    private void updateTimelineImpl(Schedule<TaxiTask> schedule, double newTaskEndTime)
    {
        Task currentTask = schedule.getCurrentTask();
        if (currentTask.getEndTime() == newTaskEndTime) {
            return;
        }

        currentTask.setEndTime(newTaskEndTime);

        List<TaxiTask> tasks = schedule.getTasks();
        int startIdx = currentTask.getTaskIdx() + 1;
        double t = newTaskEndTime;

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxiTask task = tasks.get(i);

            switch (task.getTaxiTaskType()) {
                case STAY: {
                    if (i == tasks.size() - 1) {// last task
                        task.setBeginTime(t);
                        //even if endTime=beginTime, do not remove this task!!! A taxi schedule should end with WAIT 
                        task.setEndTime(Math.max(t, schedule.getVehicle().getT1()));
                    }
                    else {
                        // if this is not the last task then some other task (e.g. DRIVE or PICKUP)
                        // must have been added at time submissionTime <= t
                        double endTime = task.getEndTime();
                        if (endTime <= t) {// may happen if the previous task is delayed
                            schedule.removeTask(task);
                            i--;
                        }
                        else {
                            task.setBeginTime(t);
                            t = endTime;
                        }
                    }

                    break;
                }

                case DRIVE_EMPTY:
                case DRIVE_OCCUPIED: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask)task)
                            .getPath();
                    t += path.getTravelTime(); //TODO one may consider recalculation of SP!!!!
                    task.setEndTime(t);

                    break;
                }

                case PICKUP: {
                    task.setBeginTime(t);// t == taxi's arrival time
                    double t0 = ((TaxiPickupTask)task).getRequest().getT0();// t0 == passenger's departure time
                    t = Math.max(t, t0) + params.pickupDuration; // the true pickup starts at max(t, t0)
                    task.setEndTime(t);

                    break;
                }
                case DROPOFF: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += params.dropoffDuration;
                    task.setEndTime(t);

                    break;
                }
            }
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
        for (Vehicle veh : context.getVrpData().getVehicles().values()) {
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

                TaxiTask lastTask = schedule.getTasks().get(newLastTaskIdx);
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

                    case DRIVE_EMPTY:
                        if (!params.vehicleDiversion) {
                            throw new RuntimeException("Currently won't happen");
                        }

                        //diversion -- no STAY afterwards
                        return;

                    default:
                        throw new RuntimeException();
                }

            case PLANNED:
                removePlannedTasks(schedule, -1);
                Vehicle veh = schedule.getVehicle();
                schedule.addTask(new TaxiStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
                return;

            case COMPLETED:
                return;

            case UNPLANNED:
                throw new IllegalStateException();
        }
    }


    private Integer countUnremovablePlannedTasks(Schedule<TaxiTask> schedule)
    {
        TaxiTask currentTask = schedule.getCurrentTask();
        switch (currentTask.getTaxiTaskType()) {
            case PICKUP:
                return params.destinationKnown ? 2 : null;

            case DRIVE_OCCUPIED:
                return 1;

            case DRIVE_EMPTY:
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


    private void removePlannedTasks(Schedule<TaxiTask> schedule, int newLastTaskIdx)
    {
        List<TaxiTask> tasks = schedule.getTasks();

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxiTask task = tasks.get(i);
            schedule.removeTask(task);

            if (task instanceof TaxiTaskWithRequest) {
                TaxiTaskWithRequest taskWithReq = (TaxiTaskWithRequest)task;
                taskWithReq.removeFromRequest();

                if (task.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                    removedRequests.add(taskWithReq.getRequest());
                }
            }
        }
    }
}
