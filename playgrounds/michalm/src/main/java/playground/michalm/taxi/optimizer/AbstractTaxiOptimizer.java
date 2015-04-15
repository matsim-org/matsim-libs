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
            for (Vehicle v : optimConfig.context.getVrpData().getVehicles()) {
                optimConfig.scheduler.updateTimeline(TaxiSchedules.asTaxiSchedule(v.getSchedule()));
            }

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
        Schedule<TaxiTask> taxiSchedule = TaxiSchedules.asTaxiSchedule(schedule);
        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);
        
        TaxiTask newCurrentTask = taxiSchedule.nextTask();
        
        if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
            requiresReoptimization = doReoptimizeAfterNextTask(newCurrentTask);
        }
    }
    
    
    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return !optimConfig.scheduler.getParams().destinationKnown && 
            (newCurrentTask.getTaxiTaskType() == TaxiTaskType.DRIVE_WITH_PASSENGER);
    }
    


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        optimConfig.scheduler.updateTimeline(TaxiSchedules.asTaxiSchedule(driveTask.getSchedule()));

        //TODO we may here possibly decide whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
