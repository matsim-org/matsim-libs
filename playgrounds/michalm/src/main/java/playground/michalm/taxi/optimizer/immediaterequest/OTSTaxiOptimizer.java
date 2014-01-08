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

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.schedule.Schedule;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.schedule.TaxiTask;


public class OTSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final TaxiOptimizationPolicy optimizationPolicy;


    public OTSTaxiOptimizer(VrpData data, VrpPathCalculator calculator, Params params,
            TaxiOptimizationPolicy optimizationPolicy)
    {
        super(data, calculator, params);
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        if (!scheduleUpdated) {// no changes
            return false;
        }

        return optimizationPolicy.shouldOptimize(schedule.getCurrentTask());
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        return false;
    }
}
