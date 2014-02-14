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
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class OTSTaxiOptimizer
    implements ImmediateRequestTaxiOptimizer

{
    public static final Comparator<TaxiRequest> SUBMISSION_TIME_COMPARATOR = new Comparator<TaxiRequest>() {
        public int compare(TaxiRequest r1, TaxiRequest r2)
        {
            return Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime());
        }
    };

    protected final MatsimVrpContext context;
    protected final TaxiScheduler scheduler;

    protected final Queue<TaxiRequest> unplannedRequests;

    private boolean requiresReoptimization = false;


    public OTSTaxiOptimizer(TaxiScheduler scheduler, MatsimVrpContext context)
    {
        this.context = context;
        this.scheduler = scheduler;

        int initialCapacity = context.getVrpData().getVehicles().size();//1 awaiting req/veh
        unplannedRequests = new PriorityQueue<TaxiRequest>(initialCapacity,
                SUBMISSION_TIME_COMPARATOR);
    }


    protected void scheduleUnplannedRequests()
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = scheduler.findBestVehicleRequestPath(req, context
                    .getVrpData().getVehicles());

            if (best == null) {
                return;
            }

            scheduler.scheduleRequest(best);
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

        scheduler.updateBeforeNextTask(taxiSchedule);
        TaxiTask nextTask = taxiSchedule.nextTask();

        if (!scheduler.getParams().destinationKnown) {
            if (nextTask != null // == schedule COMPLETED
                    && nextTask.getTaxiTaskType() == TaxiTaskType.DROPOFF_DRIVE) {
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
        scheduler.updateCurrentAndPlannedTasks(schedule, predictedEndTime);

        //we may here possibly decide here whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }


    @Override
    public TaxiScheduler getScheduler()
    {
        return scheduler;
    }

}
