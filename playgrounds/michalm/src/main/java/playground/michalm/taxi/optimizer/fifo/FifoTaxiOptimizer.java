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


public class FifoTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    public static FifoTaxiOptimizer createOptimizerWithoutRescheduling(
            TaxiOptimizerConfiguration optimConfig)
    {
        return new FifoTaxiOptimizer(optimConfig, false);
    }


    public static FifoTaxiOptimizer createOptimizerWithRescheduling(
            TaxiOptimizerConfiguration optimConfig)
    {
        return new FifoTaxiOptimizer(optimConfig, true);
    }


    private FifoTaxiOptimizer(TaxiOptimizerConfiguration optimConfig,
            boolean rescheduleAwaitingRequests)
    {
        super(optimConfig, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR),
                rescheduleAwaitingRequests);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        new FifoSchedulingProblem(optimConfig)
                .scheduleUnplannedRequests((Queue<TaxiRequest>)unplannedRequests);
    }
}
