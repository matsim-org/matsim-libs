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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.schedule.Schedule;

import playground.michalm.taxi.schedule.TaxiTask;


public class OTSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final TaxiOptimizationPolicy optimizationPolicy;


    public OTSTaxiOptimizer(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params, TaxiOptimizationPolicy optimizationPolicy)
    {
        super(context, calculator, params);
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected void nextTask(Schedule<TaxiTask> schedule, boolean scheduleUpdated)
    {
        if (scheduleUpdated && optimizationPolicy.shouldOptimize(schedule.getCurrentTask())) {
            scheduleUnplannedRequests();
        }

        schedule.nextTask();
    }
}
