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

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.network.impl.LinkTimePair;
import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import playground.michalm.taxi.model.TaxiRequest;
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
    extends AbstractTaxiOptimizer
    implements VrpOptimizerWithOnlineTracking
{
    protected static class VehicleDrive
    {
        public static final VehicleDrive NO_VEHICLE_DRIVE_FOUND = new VehicleDrive(null, null,
                Integer.MAX_VALUE / 2, Integer.MAX_VALUE);

        private final Vehicle vehicle;
        private final Arc arc;
        private final int t1;
        private final int t2;


        protected VehicleDrive(Vehicle vehicle, Arc arc, int t1, int t2)
        {
            this.vehicle = vehicle;
            this.arc = arc;
            this.t1 = t1;
            this.t2 = t2;
        }
    }


    private TaxiDelaySpeedupStats delaySpeedupStats;
    private final boolean destinationKnown;
    private final boolean minimizePickupTripTime;
    private final int pickupDuration;
    private final int dropoffDuration;


    public ImmediateRequestTaxiOptimizer(VrpData data, boolean destinationKnown,
            boolean minimizePickupTripTime, int pickupDuration, int dropoffDuration)
    {
        super(data);
        this.destinationKnown = destinationKnown;
        this.minimizePickupTripTime = minimizePickupTripTime;
        this.pickupDuration = pickupDuration;
        this.dropoffDuration = dropoffDuration;
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    @Override
    protected void scheduleRequest(TaxiRequest request)
    {
        VehicleDrive bestVehicle = findBestVehicle(request, data.getVehicles());

        if (bestVehicle != VehicleDrive.NO_VEHICLE_DRIVE_FOUND) {
            scheduleRequestImpl(bestVehicle, request);
        }
    }


    protected VehicleDrive findBestVehicle(TaxiRequest req, List<Vehicle> vehicles)
    {
        int currentTime = data.getTime();
        VehicleDrive best = VehicleDrive.NO_VEHICLE_DRIVE_FOUND;

        for (Vehicle veh : vehicles) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);

            // COMPLETED or STARTED but delayed (time window T1 exceeded)
            if (schedule.getStatus() == ScheduleStatus.COMPLETED
                    || currentTime >= Schedules.getActualT1(schedule)) {
                // skip this vehicle
                continue;
            }

            // status = UNPLANNED/PLANNED/STARTED
            LinkTimePair departure = calculateDeparture(schedule, currentTime);

            if (departure == null) {
                continue;
            }

            int t1 = departure.getTime();
            Arc arc = data.getVrpGraph().getArc(departure.getLink(), req.getFromLink());
            int t2 = t1 + arc.getTimeOnDeparture(departure.getTime());

            if (minimizePickupTripTime) {
                if (t2 - t1 < best.t2 - best.t1) {
                    // TODO: in the future: add a check if the taxi time windows are satisfied
                    best = new VehicleDrive(veh, arc, t1, t2);
                }
            }
            else {
                if (t2 < best.t2) {
                    // TODO: in the future: add a check if the taxi time windows are satisfied
                    best = new VehicleDrive(veh, arc, t1, t2);
                }
            }
        }

        return best;
    }


    protected LinkTimePair calculateDeparture(Schedule<TaxiTask> schedule, int currentTime)
    {
        Link link;
        int time;

        switch (schedule.getStatus()) {
            case UNPLANNED:
                Vehicle vehicle = schedule.getVehicle();
                link = vehicle.getDepot().getLink();
                time = Math.max(vehicle.getT0(), currentTime);
                return new LinkTimePair(link, time);

            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), currentTime);
                        return new LinkTimePair(link, time);

                    case PICKUP_STAY:
                        if (!destinationKnown) {
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


    protected void scheduleRequestImpl(VehicleDrive best, TaxiRequest req)
    {
        Schedule<TaxiTask> bestSched = TaxiSchedules.getSchedule(best.vehicle);

        if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
            TaxiWaitStayTask lastTask = (TaxiWaitStayTask)Schedules.getLastTask(bestSched);// only WAIT

            switch (lastTask.getStatus()) {
                case PLANNED:
                    if (lastTask.getBeginTime() == best.t1) { // waiting for 0 seconds!!!
                        bestSched.removeLastTask();// remove WaitTask
                    }
                    else {
                        // TODO actually this WAIT task will not be performed
                        // so maybe we can remove it right now?

                        lastTask.setEndTime(best.t1);// shortening the WAIT task
                    }
                    break;

                case STARTED:
                    lastTask.setEndTime(best.t1);// shortening the WAIT task
                    break;

                case PERFORMED:
                default:
                    throw new IllegalStateException();
            }
        }

        bestSched.addTask(new TaxiPickupDriveTask(best.t1, best.t2, best.arc, req));

        int t3 = best.t2 + pickupDuration;
        bestSched.addTask(new TaxiPickupStayTask(best.t2, t3, req));

        if (destinationKnown) {
            appendDropoffAfterPickup(bestSched);
            appendWaitAfterDropoff(bestSched);
        }
    }


    @Override
    protected boolean updateBeforeNextTask(Schedule<TaxiTask> schedule)
    {
        int time = data.getTime();
        TaxiTask currentTask = schedule.getCurrentTask();

        int initialEndTime;

        if (currentTask.getType() == TaskType.DRIVE) {
            initialEndTime = ((DriveTask)currentTask).getVehicleTracker().getInitialEndTime();
        }
        else {
            initialEndTime = currentTask.getEndTime();
        }

        int delay = time - initialEndTime;

        if (delay != 0) {
            if (delaySpeedupStats != null) {// optionally, one may record delays
                delaySpeedupStats.updateStats(currentTask, delay);
            }
        }

        if (currentTask.getEndTime() != time) {
            currentTask.setEndTime(time);
            updatePlannedTasks(schedule);
        }

        if (!destinationKnown) {
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
        int t3 = pickupStayTask.getEndTime();

        Arc arc = data.getVrpGraph().getArc(reqFromLink, reqToLink);
        int t4 = t3 + arc.getTimeOnDeparture(t3);
        schedule.addTask(new TaxiDropoffDriveTask(t3, t4, arc, req));

        int t5 = t4 + dropoffDuration;
        schedule.addTask(new TaxiDropoffStayTask(t4, t5, req));
    }


    protected void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
    {
        TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules.getLastTask(schedule);

        // addWaitTime at the end (even 0-second WAIT)
        int t5 = dropoffStayTask.getEndTime();
        int tEnd = Math.max(t5, Schedules.getActualT1(schedule));
        Link link = dropoffStayTask.getLink();

        schedule.addTask(new TaxiWaitStayTask(t5, tEnd, link));
    }


    /**
     * @param schedule
     * @return {@code true} if something has been changed; otherwise {@code false}
     */
    protected void updatePlannedTasks(Schedule<TaxiTask> schedule)
    {
        Task currentTask = schedule.getCurrentTask();
        List<TaxiTask> tasks = schedule.getTasks();

        int startIdx = currentTask.getTaskIdx() + 1;
        int t = currentTask.getEndTime();

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxiTask task = tasks.get(i);

            switch (task.getTaxiTaskType()) {
                case WAIT_STAY: {
                    // wait can be at the end of the schedule
                    //
                    // BUT:
                    //
                    // t1 - beginTime for WAIT
                    // t2 - arrival of new task
                    // t3 - updateSchedule() called -- end of the current task (STARTED)
                    // t4 - endTime for WAIT (according to some earlier plan)
                    //
                    // t1 <= t2 <= t3 < t4
                    //
                    // it may also be kept in situation where we have WAIT for [t1;t4) and at t2
                    // (t1 < t2 < t4) arrives new tasks are to be added but WAIT is still PLANNED
                    // (not STARTED) since there has been a delay in performing some previous tasks
                    // so we shorten WAIT to [t1;t2) and plan the newly arrived task at [t2; t2+x)
                    // but of course there will probably be a delay in performing this task, as
                    // we do not know when exactly the current task (one that is STARTED) will end
                    // (t3).
                    //
                    // Of course, the WAIT task will never be performed as we have now time t2
                    // and it is planned for [t1;t2); it will be removed on the nearest
                    // updateSchedule(), just like here:

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
                        // at time <= t
                        // THEREFORE: task.endTime() <= t, and so it can be removed
                        schedule.removeTask(task);
                        i--;
                    }

                    break;
                }

                case PICKUP_DRIVE:
                case DROPOFF_DRIVE:
                case CRUISE_DRIVE: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += ((DriveTask)task).getArc().getTimeOnDeparture(t);
                    task.setEndTime(t);

                    break;
                }
                case PICKUP_STAY: {
                    task.setBeginTime(t);// t == taxi's arrival time
                    int t0 = ((TaxiPickupStayTask)task).getRequest().getT0();// t0 == passenger's departure time
                    t = Math.max(t, t0) + pickupDuration; // the true pickup starts at max(t, t0)
                    task.setEndTime(t);

                    break;
                }
                case DROPOFF_STAY: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += dropoffDuration;
                    task.setEndTime(t);

                    break;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }


    @Override
    public boolean nextPositionReached(VehicleTracker vehicleTracker)
    {
        DriveTask dt = vehicleTracker.getDriveTask();
        dt.setEndTime(vehicleTracker.predictEndTime(data.getTime()));

        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)dt.getSchedule();
        updatePlannedTasks(schedule);

        return false;
    }
}
