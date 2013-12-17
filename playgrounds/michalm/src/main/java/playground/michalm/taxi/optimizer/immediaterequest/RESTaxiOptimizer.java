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

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class RESTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final VrpData data;
    private final boolean destinationKnown;
    private final TaxiOptimizationPolicy optimizationPolicy;


    public RESTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            int pickupDuration, TaxiOptimizationPolicy optimizationPolicy)
    {
        super(data, destinationKnown, minimizePickupTripTime, pickupDuration);
        this.data = data;
        this.destinationKnown = destinationKnown;
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        if (!scheduleUpdated) {// no changes
            return false;
        }

        return optimizationPolicy.shouldOptimize(schedule.getCurrentTask());
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected void optimize()
    {
        for (Vehicle veh : data.getVehicles()) {
            removePlannedRequests(TaxiSchedules.getSchedule(veh));
        }

        super.optimize();
    }


    private void removePlannedRequests(Schedule<TaxiTask> schedule)
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
                        if (!destinationKnown) {
                            return;
                        }

                        obligatoryTasks = 3;
                        break;

                    case PICKUP_STAY:
                        if (!destinationKnown) {
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
                        // this WAIT is not the last task, so it seems that it is delayed
                        // and there are some other planned task
                        if (!TaxiUtils.isCurrentTaskDelayed(schedule, data.getTime())) {
                            throw new IllegalStateException();//
                        }
                }

                if (obligatoryTasks == 0) {
                    return;
                }

                removePlannedTasks(schedule, obligatoryTasks);

                int tEnd = Schedules.getActualT1(schedule);
                int scheduleEndTime = schedule.getEndTime();
                Vertex lastVertex = Schedules.getLastVertexInSchedule(schedule);

                if (scheduleEndTime < tEnd) {
                    schedule.addTask(new TaxiWaitStayTask(scheduleEndTime, tEnd, lastVertex));
                }
                else {
                    // may happen that the previous task ends after tEnd!!!!!!!!!!
                    // just a hack to comply with the assumptions, i.e. lastTask is WAIT_TASK
                    schedule.addTask(new TaxiWaitStayTask(scheduleEndTime, scheduleEndTime,
                            lastVertex));
                }
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


    private void removePlannedTasks(Schedule<TaxiTask> schedule, int obligatoryTasks)
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
                    unplannedRequestQueue.add(taskWithReq.getRequest());
                }
            }
        }
    }
}
