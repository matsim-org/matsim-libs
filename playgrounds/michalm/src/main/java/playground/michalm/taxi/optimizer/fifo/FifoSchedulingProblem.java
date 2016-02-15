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

package playground.michalm.taxi.optimizer.fifo;

import java.util.Queue;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;


public class FifoSchedulingProblem
{
    private final TaxiOptimizerContext optimContext;
    private final BestDispatchFinder dispatchFinder;


    public FifoSchedulingProblem(TaxiOptimizerContext optimContext,
            BestDispatchFinder vrpFinder)
    {
        this.optimContext = optimContext;
        this.dispatchFinder = vrpFinder;
    }


    public void scheduleUnplannedRequests(Queue<TaxiRequest> unplannedRequests)
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            BestDispatchFinder.Dispatch best = dispatchFinder.findBestVehicleForRequest(req,
                    optimContext.context.getVrpData().getVehicles().values());
            
            //TODO search only through available vehicles
            //TODO what about k-nearstvehicle filtering?

            if (best == null) {//TODO won't work with req filtering; use VehicleData to find out when to exit???
                return;
            }

            optimContext.scheduler.scheduleRequest(best.vehicle, best.request, best.path);
            unplannedRequests.poll();
        }
    }
}
