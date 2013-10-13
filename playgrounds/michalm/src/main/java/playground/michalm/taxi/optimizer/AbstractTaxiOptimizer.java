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

package playground.michalm.taxi.optimizer;

import java.util.*;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.RequestStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.taxi.schedule.*;


/**
 * The assumptions made:
 * <p>
 * a. all requests are queued according to the time of arrival (this implies the priority)
 * <p>
 * b. requests are scheduled one-by-one; while scheduling request A, other requests queued are not
 * taken into account
 * 
 * @author michalm
 */
public abstract class AbstractTaxiOptimizer
    implements VrpOptimizer
{
    protected final VrpData data;
    protected Queue<Request> unplannedRequestQueue;


    public AbstractTaxiOptimizer(VrpData data)
    {
        this.data = data;
        int initialCapacity = data.getVehicles().size();// just proportional to the number of
                                                        // vehicles (for easy scaling)
        unplannedRequestQueue = new PriorityQueue<Request>(initialCapacity,
                new Comparator<Request>() {
                    public int compare(Request r1, Request r2)
                    {
                        return r1.getId() - r2.getId();
                    }
                });
    }


    /**
     */
    @Override
    public void init()
    {
        // Initial remark!!!
        // We start with 0 requests so there is no need for any pre-optimization.
        // Let's just add a WAIT task to each schedule

        for (Vehicle veh : (List<Vehicle>)data.getVehicles()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);

            schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getDepot()
                    .getVertex()));
        }
    }


    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequestQueue.add(request);

        optimize();
    }


    protected abstract boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated);


    protected abstract boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated);


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;
        boolean scheduleUpdated = false;

        // Assumption: there is no delay as long as the schedule has not been started (PLANNED)
        if (schedule.getStatus() == ScheduleStatus.STARTED) {
            scheduleUpdated = updateBeforeNextTask(taxiSchedule);
        }

        if (shouldOptimizeBeforeNextTask(taxiSchedule, scheduleUpdated)) {
            optimize();
        }

        schedule.nextTask();

        if (shouldOptimizeAfterNextTask(taxiSchedule, scheduleUpdated)) {// after nextTask()
            optimize();
        }
    }


    /**
     * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e.
     * beginTime and endTime) of all tasks in the schedule.
     * 
     * @return <code>true</code> if there have been significant changes in the schedule hence the
     *         schedule needs to be re-optimized
     */
    protected abstract boolean updateBeforeNextTask(Schedule<TaxiTask> schedule);


    /**
     * Try to schedule all unplanned tasks (if any)
     */
    protected void optimize()
    {
        while (!unplannedRequestQueue.isEmpty()) {
            Request req = unplannedRequestQueue.peek();

            if (req.getStatus() != RequestStatus.UNPLANNED) {
                throw new IllegalStateException();
            }

            scheduleRequest(req);// means: try to schedule

            if (req.getStatus() == RequestStatus.UNPLANNED) {
                return;// no taxi available
            }
            else {
                unplannedRequestQueue.poll();
            }
        }
    }


    protected abstract void scheduleRequest(Request request);
}
