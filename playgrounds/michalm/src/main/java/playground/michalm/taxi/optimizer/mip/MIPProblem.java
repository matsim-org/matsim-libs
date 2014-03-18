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

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;


public class MIPProblem
{
    static class MIPSolution
    {
        final double[][] x;
        final double[] w;


        MIPSolution(double[][] x, double[] w)
        {
            this.x = x;
            this.w = w;
        }
    };


    private static final int REQS_PER_VEH = 99999;

    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private SortedSet<TaxiRequest> unplannedRequests;
    private MIPRequestData rData;
    private VehicleData vData;

    private MIPSolution initialSolution;
    private MIPSolution finalSolution;


    public MIPProblem(TaxiOptimizerConfiguration optimConfig,
            PathTreeBasedTravelTimeCalculator pathTravelTimeCalc)
    {
        if (!optimConfig.scheduler.getParams().destinationKnown) {
            throw new IllegalArgumentException("Destinations must be known ahead");
        }

        this.optimConfig = optimConfig;
        this.pathTravelTimeCalc = pathTravelTimeCalc;
    }


    public void scheduleUnplannedRequests(SortedSet<TaxiRequest> unplannedRequests)
    {
        this.unplannedRequests = unplannedRequests;

        initData();
        if (vData.dimension == 0 || rData.dimension == 0) {
            return;
        }

        findInitialSolution();
        solveProblem();
        scheduleSolution();
        
        TaxiStats stats = new TaxiStatsCalculator().calculateStats(optimConfig.context.getVrpData());
        System.err.println(TaxiStats.HEADER);
        System.err.println(stats.toString());
    }


    private void initData()
    {
        List<TaxiRequest> removedRequests = optimConfig.scheduler
                .removePlannedRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);

        vData = new VehicleData(optimConfig);
        if (vData.dimension == 0) {
            return;
        }

        int maxReqCount = REQS_PER_VEH * vData.dimension;
        rData = new MIPRequestData(optimConfig, unplannedRequests, maxReqCount);
        if (rData.dimension == 0) {
            return;
        }
    }


    private void findInitialSolution()
    {
        initialSolution = new MIPSolutionFinder(optimConfig, rData, vData).findInitialSolution();
        optimConfig.scheduler.removePlannedRequestsFromAllSchedules();
    }


    private void solveProblem()
    {
        finalSolution = new MIPGurobiSolver(optimConfig, pathTravelTimeCalc, rData, vData)
                .solve(initialSolution);
    }


    private void scheduleSolution()
    {
        new MIPSolutionScheduler(optimConfig, rData, vData).updateSchedules(finalSolution);
        unplannedRequests.removeAll(Arrays.asList(rData.requests));
    }


    MIPRequestData getRequestData()
    {
        return rData;
    }


    VehicleData getVehicleData()
    {
        return vData;
    }
}
