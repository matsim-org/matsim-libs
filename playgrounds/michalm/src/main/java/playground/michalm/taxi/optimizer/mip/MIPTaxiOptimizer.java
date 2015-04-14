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

    private boolean isLastPlanningHorizonFull = false;//in order to run optimization for the first request
    private boolean hasPickedUpReqsRecently = false;

    private int optimCounter = 0;


    public MIPTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR));

        if (!optimConfig.scheduler.getParams().destinationKnown) {
            throw new IllegalArgumentException("Destinations must be known ahead");
        }

        pathTravelTimeCalc = new PathTreeBasedTravelTimeCalculator(new LeastCostPathTreeStorage(
                optimConfig.context.getScenario().getNetwork()));
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        if (unplannedRequests.size() == 0) {
            //nothing new to be planned and we want to avoid extra re-planning of what has been
            //already planned (high computational cost while only marginal improvement) 
            return;
        }

        if (isLastPlanningHorizonFull && //last time we planned as many requests as possible, and...
                !hasPickedUpReqsRecently) {//...since then no empty space has appeared in the planning horizon 
            return;
        }

        MIPProblem mipProblem = new MIPProblem(optimConfig, pathTravelTimeCalc);
        mipProblem.scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);

        optimCounter++;
        if (optimCounter % 10 == 0) {
            System.err.println(optimCounter + "; time=" + optimConfig.context.getTime());
        }

        isLastPlanningHorizonFull = mipProblem.isPlanningHorizonFull();
        hasPickedUpReqsRecently = false;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);
        checkIfRequiresReoptimization(schedule);
    }


    private void checkIfRequiresReoptimization(Schedule<? extends Task> schedule)
    {
        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            return;
        }

        TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
        if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP) {
            hasPickedUpReqsRecently = true;
            requiresReoptimization = true;
        }
    }
}
