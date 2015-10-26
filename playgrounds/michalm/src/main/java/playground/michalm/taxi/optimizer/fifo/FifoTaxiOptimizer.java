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

package playground.michalm.taxi.optimizer.fifo;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestFinder;


public class FifoTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final VehicleRequestFinder vrpFinder;

    public FifoTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), true);
        vrpFinder = new VehicleRequestFinder(optimConfig);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        new FifoSchedulingProblem(optimConfig, vrpFinder)
                .scheduleUnplannedRequests((Queue<TaxiRequest>)unplannedRequests);
    }
}
