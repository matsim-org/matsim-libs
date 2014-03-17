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


    private static final int REQS_PER_VEH = 5;

    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private SortedSet<TaxiRequest> unplannedRequests;
    private MIPRequestData rData;
    private VehicleData vData;


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

        MIPSolution initialSolution = findInitialSolution();
        MIPSolution solution = solveProblem(initialSolution);
        scheduleSolution(solution);
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


    private MIPSolution findInitialSolution()
    {
        MIPSolution solution = new MIPSolutionFinder(optimConfig, rData, vData)
                .findInitialSolution();

        optimConfig.scheduler.removePlannedRequestsFromAllSchedules();

        return solution;
    }


    private MIPSolution solveProblem(MIPSolution initialSolution)
    {
        MIPGurobiSolver solver = new MIPGurobiSolver(optimConfig, pathTravelTimeCalc, rData, vData);
        return solver.solve(initialSolution);
    }


    private void scheduleSolution(MIPSolution solution)
    {
        new MIPSolutionScheduler(optimConfig, rData, vData).updateSchedules(solution);

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
