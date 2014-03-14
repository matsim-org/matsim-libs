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

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class MIPTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;


    public MIPTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR));

        pathTravelTimeCalc = new PathTreeBasedTravelTimeCalculator(new LeastCostPathTreeStorage(
                optimConfig.context.getScenario().getNetwork()));
    }


    protected void scheduleUnplannedRequests()
    {
        new MIPProblem(optimConfig, pathTravelTimeCalc)
                .scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);

        TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
        if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP_DRIVE) {
            if (unplannedRequests.size() > 0) {
                requiresReoptimization = true;
            }
        }
    }
}
