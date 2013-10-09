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

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;


public class OTSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final TaxiOptimizationPolicy optimizationPolicy;


    public OTSTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            int pickupDuration, TaxiOptimizationPolicy optimizationPolicy)
    {
        super(data, destinationKnown, minimizePickupTripTime, pickupDuration);
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        if (!scheduleUpdated) {// no changes
            return false;
        }

        return optimizationPolicy.shouldOptimize(vehicle.getSchedule().getCurrentTask());
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }
}
