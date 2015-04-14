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

package playground.michalm.taxi.optimizer;

import java.util.Collection;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public abstract class AbstractTaxiOptimizer
    implements TaxiOptimizer
{
    protected final TaxiOptimizerConfiguration optimConfig;

    protected final Collection<TaxiRequest> unplannedRequests;

    protected boolean requiresReoptimization = false;


    public AbstractTaxiOptimizer(TaxiOptimizerConfiguration optimConfig,
            Collection<TaxiRequest> unplannedRequests)
    {
        this.optimConfig = optimConfig;
        this.unplannedRequests = unplannedRequests;
    }


    protected abstract void scheduleUnplannedRequests();


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization) {
//            //TODO consider:
//            for (Vehicle v : optimConfig.context.getVrpData().getVehicles()) {
//                Schedule s = v.getSchedule();
//                
//                if (s.getStatus() == ScheduleStatus.STARTED) {
//                    double predictedEndTime = s.getCurrentTask().getTaskTracker().predictEndTime(
//                            optimConfig.context.getTime());
//                    optimConfig.scheduler.updateCurrentAndPlannedTasks(s, predictedEndTime);
//                }
//            }
//            
            scheduleUnplannedRequests();
            requiresReoptimization = false;
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

        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);
        TaxiTask newCurrentTask = taxiSchedule.nextTask();

        if (!optimConfig.scheduler.getParams().destinationKnown) {
            if (newCurrentTask != null // schedule != COMPLETED
                    && newCurrentTask.getTaxiTaskType() == TaxiTaskType.DRIVE_WITH_PASSENGER) {
                requiresReoptimization = true;
            }
        }
    }


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getTaskTracker().predictEndTime(
                optimConfig.context.getTime());
        optimConfig.scheduler.updateCurrentAndPlannedTasks(schedule, predictedEndTime);

        //TODO we may here possibly decide whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
