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


    enum Mode
    {
        OFFLINE_INIT_OPTIM(true, true, false, 99999), //
        OFFLINE_INIT(true, false, false, 99999), //
        OFFLINE_OPTIM(false, true, false, 99999), //
        OFFLINE_LOAD(false, false, true, 99999), //
        //
        ONLINE_1(true, true, false, 1), //
        ONLINE_2(true, true, false, 2), //
        ONLINE_3(true, true, false, 3), //
        ONLINE_4(true, true, false, 4), //
        ONLINE_5(true, true, false, 5);

        private final boolean init;
        private final boolean optim;
        private final boolean load;
        private final int reqsPerVeh;//planning horizon


        private Mode(boolean init, boolean optim, boolean load, int reqsPerVeh)
        {
            this.init = init;
            this.optim = optim;
            this.load = load;
            this.reqsPerVeh = reqsPerVeh;
        }
    };


    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private SortedSet<TaxiRequest> unplannedRequests;
    private MIPRequestData rData;
    private VehicleData vData;

    private MIPSolution initialSolution;
    private MIPSolution finalSolution;

    static final Mode MODE = Mode.OFFLINE_INIT_OPTIM;


    public MIPProblem(TaxiOptimizerConfiguration optimConfig,
            PathTreeBasedTravelTimeCalculator pathTravelTimeCalc)
    {
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

        if (MODE.init) {
            findInitialSolution();
        }

        if (MODE.optim) {
            solveProblem();
        }
        else if (MODE.load) {
            loadSolution(optimConfig.workingDirectory + "gurobi_solution.sol");
        }
        else if (MODE.init) {
            finalSolution = initialSolution;
        }
        else {
            throw new RuntimeException();
        }

        scheduleSolution();
    }


    private void initData()
    {
        List<TaxiRequest> removedRequests = optimConfig.scheduler
                .removeAwaitingRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);

        vData = new VehicleData(optimConfig);
        if (vData.dimension == 0) {
            return;
        }

        int maxReqCount = getPlanningHorizon();
        rData = new MIPRequestData(optimConfig, unplannedRequests, maxReqCount);
        if (rData.dimension == 0) {
            return;
        }
    }


    private MIPTaxiStats stats;


    private void findInitialSolution()
    {
        initialSolution = new MIPSolutionFinder(optimConfig, rData, vData).findInitialSolution();

        stats = new MIPTaxiStats(optimConfig.context.getVrpData());
        stats.calcInitial();

        optimConfig.scheduler.removeAwaitingRequestsFromAllSchedules();
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

        stats.calcSolved();

        try (PrintWriter pw = new PrintWriter(optimConfig.workingDirectory + "MIP_stats")) {
            stats.print(pw);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void loadSolution(String filename)
    {
        try (Scanner s = new Scanner(new File(filename))) {
            //header
            if (!s.nextLine().startsWith("# Objective value = ")) {
                throw new RuntimeException();
            }

            int n = rData.dimension;
            int m = vData.dimension;

            boolean[][] x = new boolean[m + n][m + n];
            for (int u = 0; u < m + n; u++) {
                for (int v = 0; v < m + n; v++) {

                    //line format: x_430,430 0
                    if (!s.next().equals("x_" + u + "," + v)) {
                        throw new RuntimeException();
                    }

                    x[u][v] = s.nextDouble() >= 0.5;
                }
            }

            double[] w = new double[n];
            for (int i = 0; i < n; i++) {

                //line format: w_0 22096
                if (!s.next().equals("w_" + i)) {
                    throw new RuntimeException();
                }

                w[i] = s.nextDouble();
            }

            finalSolution = new MIPSolution(x, w);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private int getPlanningHorizon()
    {
        return vData.dimension * MODE.reqsPerVeh;
    }


    private int getPlannedRequestCount()
    {
        return rData.dimension;
    }
    
    
    boolean isPlanningHorizonFull()
    {
        return getPlanningHorizon() == getPlannedRequestCount();
    }
}
