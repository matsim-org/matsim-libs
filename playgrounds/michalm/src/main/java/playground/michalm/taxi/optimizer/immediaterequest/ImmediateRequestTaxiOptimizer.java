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

package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;

import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


/**
 * The main assumption: Requests are scheduled as the last request in the schedule (of a given
 * vehicle)
 * <p>
 * However, different strategies/policies may be used for:
 * <li>vehicle selection set (only idle, idle+delivering, all)
 * <li>rescheduling on/off (a reaction t changes in a schedule after updating it)
 * 
 * @author michalm
 */
public abstract class ImmediateRequestTaxiOptimizer
    implements VrpOptimizerWithOnlineTracking
{
    protected final VrpPathCalculator calculator;
    protected final ImmediateRequestParams params;
    protected final MatsimVrpContext context;

    protected final Queue<TaxiRequest> unplannedRequests;
    protected final Comparator<VrpPathWithTravelData> pathComparator;

    private TaxiDelaySpeedupStats delaySpeedupStats;


    public ImmediateRequestTaxiOptimizer(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params)
    {
        this.calculator = calculator;
        this.params = params;
        this.context = context;

        int initialCapacity = context.getVrpData().getVehicles().size();//1 awaiting req/veh
        unplannedRequests = new PriorityQueue<TaxiRequest>(initialCapacity,
                new Comparator<TaxiRequest>() {
                    public int compare(TaxiRequest r1, TaxiRequest r2)
                    {
                        return Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime());
                    }
                });

        pathComparator = params.minimizePickupTripTime ? //
                VrpPathWithTravelDataComparators.TRAVEL_TIME_COMPARATOR : //
                VrpPathWithTravelDataComparators.ARRIVAL_TIME_COMPARATOR;

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
            schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    ////========================================================

    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequests.add((TaxiRequest)request);
        scheduleUnplannedRequests();
        
        //????
        TaxiScheduleValidator.assertNotIdleVehiclesAndUnplannedRequests(context);
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;

        boolean scheduleUpdated = updateBeforeNextTask(taxiSchedule);
        nextTask(taxiSchedule, scheduleUpdated);
    }


    protected abstract void nextTask(Schedule<TaxiTask> schedule, boolean scheduleUpdated);


    /**
     * Try to schedule all unplanned tasks (if any)
     */
    protected void scheduleUnplannedRequests()
    {
        if (unplannedRequests.isEmpty()) {
            return;
        }

        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = findBestVehicleRequestPath(req);

            if (best == null) {
                return;//no taxi available
            }

            scheduleRequestImpl(best);
            unplannedRequests.poll();
        }
    }


    ////========================================================

    protected VehicleRequestPath findBestVehicleRequestPath(TaxiRequest req)
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


    protected VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxiRequest req)
    {
        double currentTime = context.getTime();
        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);

        // COMPLETED or STARTED but delayed (time window T1 exceeded)
        if (schedule.getStatus() == ScheduleStatus.COMPLETED || currentTime >= veh.getT1()) {
            return null;// skip this vehicle
        }

        // status = UNPLANNED/PLANNED/STARTED
        Link link;
        double time;

        switch (schedule.getStatus()) {
            case UNPLANNED:
                link = veh.getStartLink();
                time = Math.max(veh.getT0(), currentTime);
                return calculator.calcPath(link, req.getFromLink(), time);

            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), currentTime);
                        return calculator.calcPath(link, req.getFromLink(), time);

                    case PICKUP_STAY:
                        if (!params.destinationKnown) {
                            return null;
                        }

                    default:
                        throw new IllegalStateException();
                }

            case COMPLETED:
            default:
                throw new IllegalStateException();
        }
    }


    protected void scheduleRequestImpl(VehicleRequestPath best)
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
                        // TODO actually this WAIT task will not be performed
                        // so maybe we can remove it right now?

                        lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task
                        
                        System.err.println("Hmmmmmmmmmmm");
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

        double t3 = best.path.getArrivalTime() + params.pickupDuration;
        bestSched.addTask(new TaxiPickupStayTask(best.path.getArrivalTime(), t3, best.request));

        if (params.destinationKnown) {
            appendDropoffAfterPickup(bestSched);
            appendWaitAfterDropoff(bestSched);
        }
    }


    /**
     * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e.
     * beginTime and endTime) of all tasks in the schedule.
     * 
     * @return <code>true</code> if there have been significant changes in the schedule hence the
     *         schedule needs to be re-optimized
     */
    protected boolean updateBeforeNextTask(Schedule<TaxiTask> schedule)
    {
        // Assumption: there is no delay as long as the schedule has not been started (PLANNED)
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        double time = context.getTime();
        TaxiTask currentTask = schedule.getCurrentTask();

        double plannedEndTime;

        if (currentTask.getType() == TaskType.DRIVE) {
            plannedEndTime = ((DriveTask)currentTask).getVehicleTracker().getPlannedEndTime();
        }
        else {
            plannedEndTime = currentTask.getEndTime();
        }

        double delay = time - plannedEndTime;

        if (delay != 0) {
            if (delaySpeedupStats != null) {// optionally, one may record delays
                delaySpeedupStats.updateStats(currentTask, delay);
            }
        }

        updateCurrentAndPlannedTasks(schedule, time);

        if (!params.destinationKnown) {
            if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP_STAY) {
                appendDropoffAfterPickup(schedule);
                appendWaitAfterDropoff(schedule);
                return true;
            }
        }

        // return delay != 0;//works only for offline vehicle tracking

        //since we can change currentTask.endTime continuously, it is hard to determine
        //what endTime was at the moment of last reoptimization (triggered by other vehicles or
        //requests)
        return true;
    }


    protected void appendDropoffAfterPickup(Schedule<TaxiTask> schedule)
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


    protected void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
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
    protected void updateCurrentAndPlannedTasks(Schedule<TaxiTask> schedule,
            double currentTaskEndTime)
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


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getVehicleTracker().predictEndTime(context.getTime());
        updateCurrentAndPlannedTasks(schedule, predictedEndTime);
    }
}
