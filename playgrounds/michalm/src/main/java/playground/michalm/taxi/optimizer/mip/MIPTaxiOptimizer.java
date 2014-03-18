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

package playground.michalm.taxi.optimizer.mip;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class MIPTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private int plannedReqs;
    private int schedulableVehs;

    private int startedReqs;


    public MIPTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR));

        pathTravelTimeCalc = new PathTreeBasedTravelTimeCalculator(new LeastCostPathTreeStorage(
                optimConfig.context.getScenario().getNetwork()));
    }


    protected void scheduleUnplannedRequests()
    {
        MIPProblem mipProblem = new MIPProblem(optimConfig, pathTravelTimeCalc);
        mipProblem.scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);

        plannedReqs = mipProblem.getRequestData().dimension;
        schedulableVehs = mipProblem.getVehicleData().dimension;

        startedReqs = 0;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);

        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
        if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP_DRIVE) {
            startedReqs++;

            if (unplannedRequests.size() > 0) {
                //requiresReoptimization |= doReoptimize();
                requiresReoptimization = true;
            }
        }
    }


    @SuppressWarnings("unused")
    private boolean doReoptimize()
    {
        int currentPlanned = plannedReqs - startedReqs;
        double currentReqsPerVeh = (double)currentPlanned / schedulableVehs;

        if (currentReqsPerVeh <= 2) {
            return true;
        }
        else {
            return startedReqs >= schedulableVehs;
        }
    }
}
