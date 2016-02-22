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

package org.matsim.contrib.taxi.optimizer.fifo;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;


public class FifoTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final BestDispatchFinder dispatchFinder;

    public FifoTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        super(optimContext, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), true);
        dispatchFinder = new BestDispatchFinder(optimContext);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        new FifoSchedulingProblem(optimContext, dispatchFinder)
                .scheduleUnplannedRequests((Queue<TaxiRequest>)unplannedRequests);
    }
}
