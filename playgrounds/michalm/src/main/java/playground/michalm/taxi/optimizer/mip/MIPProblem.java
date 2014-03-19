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

import java.io.*;
import java.util.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;


public class MIPProblem
{
    static class MIPSolution
    {
        final boolean[][] x;
        final double[] w;


        MIPSolution(boolean[][] x, double[] w)
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

        //only for OFFLINE
        MIPTaxiStats.currentStats = new MIPTaxiStats(optimConfig.context.getVrpData());
        MIPTaxiStats.currentStats.calcInitial();

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

        //only for OFFLINE
        MIPTaxiStats.currentStats.calcSolved();
    }


    @SuppressWarnings("unused")
    private MIPSolution readFromFile(String filename)
    {
        Scanner s;
        try {
            s = new Scanner(new File(filename));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //header
        if (!s.nextLine().startsWith("# Objective value = ")) {
            s.close();
            throw new RuntimeException();
        }

        int n = rData.dimension;
        int m = vData.dimension;

        boolean[][] x = new boolean[m + n][m + n];
        for (int u = 0; u < m + n; u++) {
            for (int v = 0; v < m + n; v++) {

                //line format: x_430,430 0
                if (!s.next().equals("x_" + u + "," + v)) {
                    s.close();
                    throw new RuntimeException();
                }

                x[u][v] = s.nextDouble() >= 0.5;
            }
        }

        double[] w = new double[n];
        for (int i = 0; i < n; i++) {

            //line format: w_0 22096
            if (!s.next().equals("w_" + i)) {
                s.close();
                throw new RuntimeException();
            }

            w[i] = s.nextDouble();
        }

        s.close();
        return new MIPSolution(x, w);
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
