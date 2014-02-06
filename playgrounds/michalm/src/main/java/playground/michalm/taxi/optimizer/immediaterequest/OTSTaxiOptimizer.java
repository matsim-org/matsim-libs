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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class OTSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
    implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener
{
    protected final Queue<TaxiRequest> unplannedRequests;

    private boolean requiresReoptimization = false;


    public OTSTaxiOptimizer(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params)
    {
        super(context, calculator, params);

        int initialCapacity = context.getVrpData().getVehicles().size();//1 awaiting req/veh
        unplannedRequests = new PriorityQueue<TaxiRequest>(initialCapacity,
                new Comparator<TaxiRequest>() {
                    public int compare(TaxiRequest r1, TaxiRequest r2)
                    {
                        return Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime());
                    }
                });
    }


    /**
     * Try to schedule all unplanned tasks (if any)
     */
    protected void scheduleUnplannedRequests()
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = findBestVehicleRequestPath(req);

            if (best == null) {
                return;
            }

            scheduleRequestImpl(best);
            unplannedRequests.poll();
        }
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization) {
            scheduleUnplannedRequests();
        }
    }


    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequests.add((TaxiRequest)request);
        requiresReoptimization = true;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;

        updateBeforeNextTask(taxiSchedule);
        TaxiTask nextTask = taxiSchedule.nextTask();

        if (!params.destinationKnown) {
            if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
                return;
            }
            else if (nextTask.getTaxiTaskType() == TaxiTaskType.DROPOFF_DRIVE) {
                requiresReoptimization = true;
            }
        }
    }


    //TODO switch on/off
    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getTaskTracker().predictEndTime(context.getTime());
        updateCurrentAndPlannedTasks(schedule, predictedEndTime);
    }
}
