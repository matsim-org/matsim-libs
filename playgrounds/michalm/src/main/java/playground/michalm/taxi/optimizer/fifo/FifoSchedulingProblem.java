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
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.vehreqpath.*;


public class FifoSchedulingProblem
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final VehicleRequestPathFinder vrpFinder;


    public FifoSchedulingProblem(TaxiOptimizerConfiguration optimConfig,
            VehicleRequestPathFinder vrpFinder)
    {
        this.optimConfig = optimConfig;
        this.vrpFinder = vrpFinder;
    }


    public void scheduleUnplannedRequests(Queue<TaxiRequest> unplannedRequests)
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = vrpFinder.findBestVehicleForRequest(req,
                    optimConfig.context.getVrpData().getVehicles().values());

            if (best == null) {//TODO won't work with req filtering; use VehicleData to find out when to exit???
                return;
            }

            optimConfig.scheduler.scheduleRequest(best);
            unplannedRequests.poll();
        }
    }
}
