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

package org.matsim.contrib.taxi.optimizer.mip;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.fifo.FifoSchedulingProblem;
import org.matsim.contrib.taxi.optimizer.mip.MIPProblem.MIPSolution;
import org.matsim.contrib.taxi.schedule.*;

import com.google.common.collect.Iterables;


class MIPSolutionFinder
{
    private final TaxiOptimizerContext optimContext;
    private final MIPRequestData rData;
    private final VehicleData vData;


    MIPSolutionFinder(TaxiOptimizerContext optimContext, MIPRequestData rData, VehicleData vData)
    {
        this.optimContext = optimContext;
        this.rData = rData;
        this.vData = vData;
    }


    MIPSolution findInitialSolution()
    {
        final int m = vData.getSize();
        final int n = rData.dimension;

        final boolean[][] x = new boolean[m + n][m + n];
        final double[] w = new double[n];

        Queue<TaxiRequest> queue = new PriorityQueue<>(n, Requests.T0_COMPARATOR);
        Collections.addAll(queue, rData.requests);

        BestDispatchFinder dispatchFinder = new BestDispatchFinder(optimContext);
        new FifoSchedulingProblem(optimContext, dispatchFinder).scheduleUnplannedRequests(queue);

        double t_P = optimContext.scheduler.getParams().pickupDuration;

        for (int k = 0; k < m; k++) {
            Schedule<TaxiTask> schedule = TaxiSchedules
                    .asTaxiSchedule(vData.getEntry(k).vehicle.getSchedule());
            Iterable<TaxiRequest> reqs = TaxiSchedules.getTaxiRequests(schedule);
            Iterable<TaxiRequest> plannedReqs = Iterables.filter(reqs, TaxiRequests.IS_PLANNED);

            int u = k;
            for (TaxiRequest r : plannedReqs) {
                int i = rData.reqIdToIdx.get(r.getId());
                int v = m + i;

                x[u][v] = true;

                double w_i = r.getPickupTask().getEndTime() - t_P;
                w[i] = w_i;

                u = v;
            }

            x[u][k] = true;
        }

        return new MIPSolution(x, w);
    }
}
