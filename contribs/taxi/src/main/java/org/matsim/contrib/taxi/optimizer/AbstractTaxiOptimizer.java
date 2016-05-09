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

package org.matsim.contrib.taxi.optimizer;

import java.util.*;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;


public abstract class AbstractTaxiOptimizer
    implements TaxiOptimizer
{
    protected final TaxiOptimizerContext optimContext;
    protected final Collection<TaxiRequest> unplannedRequests;

    private final boolean doUnscheduleAwaitingRequests;//PLANNED or TAXI_DISPATCHED
    private final boolean destinationKnown;
    private final boolean vehicleDiversion;
    private final int reoptimizationTimeStep;

    protected boolean requiresReoptimization = false;

    private static final String TAXI_OPTIMIZATION = "taxiOptim";
    private final IterationStopWatch stopWatch;


    public AbstractTaxiOptimizer(TaxiOptimizerContext optimContext,
            AbstractTaxiOptimizerParams params, Collection<TaxiRequest> unplannedRequests,
            boolean doUnscheduleAwaitingRequests)
    {
        this.optimContext = optimContext;
        this.unplannedRequests = unplannedRequests;
        this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;

        destinationKnown = optimContext.scheduler.getParams().destinationKnown;
        vehicleDiversion = optimContext.scheduler.getParams().vehicleDiversion;
        reoptimizationTimeStep = params.reoptimizationTimeStep;

        stopWatch = optimContext.matsimServices.getStopwatch();
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization && (e.getSimulationTime() % reoptimizationTimeStep == 0)) {
            stopWatch.beginOperation(TAXI_OPTIMIZATION);

            if (doUnscheduleAwaitingRequests) {
                unscheduleAwaitingRequests();
            }

            //TODO (1) use a seperate variable to decide upon updating the timeline??
            //TODO (2) update timeline only if the algo really wants to reschedule in this time step,
            //perhaps by checking if there are any unplanned requests??
            if (doUnscheduleAwaitingRequests) {
                for (Vehicle v : optimContext.taxiData.getVehicles().values()) {
                    optimContext.scheduler
                            .updateTimeline(TaxiSchedules.asTaxiSchedule(v.getSchedule()));
                }
            }

            scheduleUnplannedRequests();

            if (doUnscheduleAwaitingRequests && vehicleDiversion) {
                handleAimlessDriveTasks();
            }

            requiresReoptimization = false;
            stopWatch.endOperation(TAXI_OPTIMIZATION);
        }
    }


    protected void unscheduleAwaitingRequests()
    {
        List<TaxiRequest> removedRequests = optimContext.scheduler
                .removeAwaitingRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);
    }


    protected abstract void scheduleUnplannedRequests();


    protected void handleAimlessDriveTasks()
    {
        optimContext.scheduler.stopAllAimlessDriveTasks();
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
        Schedule<TaxiTask> taxiSchedule = TaxiSchedules.asTaxiSchedule(schedule);
        optimContext.scheduler.updateBeforeNextTask(taxiSchedule);

        TaxiTask newCurrentTask = taxiSchedule.nextTask();

        if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
            requiresReoptimization = doReoptimizeAfterNextTask(newCurrentTask);
        }
    }


    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return !destinationKnown && newCurrentTask.getTaxiTaskType() == TaxiTaskType.OCCUPIED_DRIVE;
    }


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        optimContext.scheduler
                .updateTimeline(TaxiSchedules.asTaxiSchedule(driveTask.getSchedule()));

        //TODO we may here possibly decide whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
