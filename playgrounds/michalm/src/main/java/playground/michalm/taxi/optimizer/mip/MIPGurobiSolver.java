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

import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.mip.MIPProblem.MIPSolution;


class MIPGurobiSolver
{
    private static enum Mode
    {
        OFFLINE(7200, true), //
        ONLINE(60, false);//

        private Mode(double timeLimit, boolean output)
        {
            this.timeLimit = timeLimit;
            this.output = output;
        }


        private final double timeLimit;
        private final boolean output;
    }


    private static final double W_MAX = 30 * 60 * 60;//30 hours

    private static final double TW_MAX = 1.5 * 60 * 60;// 1.5 hours (Mielec) 
    private static final double TP_MAX = 20 * 60;// 20 minutes (Mielec)

    private final TaxiOptimizerContext optimContext;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;
    private final MIPRequestData rData;
    private final VehicleData vData;
    private final int n;//request count
    private final int m;//vehicle count

    //    private GRBModel model;
    //    private GRBVar[][] xVar;//for each request/vehicle pair, (i, j)
    //    private GRBVar[] wVar; //for each request, i

    //private final Mode mode = Mode.OFFLINE;
    private final Mode mode = Mode.ONLINE;


    MIPGurobiSolver(TaxiOptimizerContext optimContext,
            PathTreeBasedTravelTimeCalculator pathTravelTimeCalc, MIPRequestData rData,
            VehicleData vData)
    {
        this.optimContext = optimContext;
        this.pathTravelTimeCalc = pathTravelTimeCalc;
        this.rData = rData;
        this.vData = vData;

        n = rData.dimension;
        m = vData.dimension;
    }


    MIPSolution solve(MIPSolution initialSolution)
    {
        return null;
    }
    //        try {
    //            model = new GRBModel(new GRBEnv());
    //
    //            // this is the internal model (a copy of that passed to the constructor)
    //            GRBEnv env = model.getEnv();
    //
    //            env.set(GRB.DoubleParam.TimeLimit, mode.timeLimit);// 2 hours
    //            //env.set(GRB.DoubleParam.MIPGap, 0.001);//0.1%
    //
    //            //env.set(GRB.IntParam.MIPFocus, 1);//the focus towards finding feasible solutions
    //            //or alternatively: focus towards finding feasible solutions after 1 hour
    //            //env.set(GRB.DoubleParam.ImproveStartTime, 3600);
    //
    //            //env.set(GRB.IntParam.Threads, 1);//number of threads
    //
    //            env.set(GRB.IntParam.OutputFlag, mode.output ? 1 : 0);//output
    //
    //            addXVariables();
    //            addWVariables();
    //            model.update();
    //
    //            setObjective();
    //
    //            addOneIncomingConstraint();
    //            addOneOutgoingConstraint();
    //
    //            addVehToReqLinConstraint();
    //            addReqToReqLinConstraint();
    //            model.update();
    //
    //            //model.write("D:\\model.lp");
    //
    //            applyInitialSolution(initialSolution);
    //
    //            model.optimize();
    //
    //            if (mode.output) {
    //                model.write(optimContext.workingDirectory + "gurobi_solution.sol");
    //            }
    //
    //            MIPSolution solution = extractSolution();
    //
    //            model.dispose();
    //            env.dispose();
    //
    //            return solution;
    //        }
    //        catch (GRBException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }
    //
    //
    //    private void addXVariables()
    //        throws GRBException
    //    {
    //        xVar = new GRBVar[m + n][m + n];
    //        for (int u = 0; u < m + n; u++) {
    //            for (int v = 0; v < m + n; v++) {
    //                xVar[u][v] = model.addVar(0, 1, 0, GRB.BINARY, "x_" + u + "," + v);
    //            }
    //        }
    //    }
    //
    //
    //    private void addWVariables()
    //        throws GRBException
    //    {
    //        wVar = new GRBVar[n];
    //        for (int i = 0; i < n; i++) {
    //            double e_i = rData.requests[i].getT0();
    //            double l_i = Math.min(e_i + TW_MAX, W_MAX);
    //            wVar[i] = model.addVar(e_i, l_i, 0, GRB.CONTINUOUS, "w_" + i);
    //        }
    //    }
    //
    //
    //    private void setObjective()
    //        throws GRBException
    //    {
    //        GRBLinExpr obj = new GRBLinExpr();
    //        for (int i = 0; i < n; i++) {
    //            obj.addTerm(1, wVar[i]);
    //
    //            double e_i = rData.requests[i].getT0();
    //            obj.addConstant(-e_i);
    //        }
    //
    //        model.setObjective(obj);
    //        model.set(GRB.IntAttr.ModelSense, 1);
    //    }
    //
    //
    //    private void addOneIncomingConstraint()
    //        throws GRBException
    //    {
    //        for (int v = 0; v < m + n; v++) {
    //            GRBLinExpr expr = new GRBLinExpr();
    //
    //            for (int u = 0; u < m + n; u++) {
    //                expr.addTerm(1, xVar[u][v]);
    //            }
    //
    //            model.addConstr(expr, GRB.EQUAL, 1, "incoming: x_u," + v);
    //        }
    //    }
    //
    //
    //    private void addOneOutgoingConstraint()
    //        throws GRBException
    //    {
    //        for (int u = 0; u < m + n; u++) {
    //            GRBLinExpr expr = new GRBLinExpr();
    //
    //            for (int v = 0; v < m + n; v++) {
    //                expr.addTerm(1, xVar[u][v]);
    //            }
    //
    //            model.addConstr(expr, GRB.EQUAL, 1, "outgoing: x_" + u + ",v");
    //        }
    //    }
    //
    //
    //    private void addVehToReqLinConstraint()
    //        throws GRBException
    //    {
    //        for (int i = 0; i < n; i++) {
    //            GRBLinExpr expr = new GRBLinExpr();
    //            expr.addTerm(1, wVar[i]);
    //
    //            Link toLink = rData.requests[i].getFromLink();
    //
    //            for (int k = 0; k < m; k++) {
    //                VehicleData.Entry departure = vData.entries.get(k);
    //
    //                double a_k = departure.time;
    //                double t_O_ki = pathTravelTimeCalc.calcTravelTime(departure.link, toLink);
    //
    //                if (doExcludeVehToReqDrive(i, a_k, t_O_ki)) {
    //                    GRBLinExpr excludeX = new GRBLinExpr();
    //                    excludeX.addTerm(1, xVar[k][m + i]);
    //                    model.addConstr(excludeX, GRB.EQUAL, 0, "v2r excluded " + k + "," + i);
    //                }
    //                else {
    //                    expr.addTerm(-a_k - t_O_ki, xVar[k][m + i]);
    //                }
    //            }
    //
    //            model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(v2r)_" + i);
    //        }
    //    }
    //
    //
    //    private boolean doExcludeVehToReqDrive(int i, double a_k, double t_O_ki)
    //    {
    //        double l_i = rData.requests[i].getT0() + TW_MAX;
    //        double earliestArrival_i = a_k + t_O_ki;
    //
    //        //a_k + t_O_ki  > l_i ==> x[k][m+i] = 0
    //        if (earliestArrival_i > l_i) {
    //            return true;
    //        }
    //
    //        //t_O_ki > MAX_TP ==> x[k][m+i] = 0
    //        if (t_O_ki > TP_MAX) {
    //            return true;
    //        }
    //
    //        return false;
    //    }
    //
    //
    //    private void addReqToReqLinConstraint()
    //        throws GRBException
    //    {
    //        TaxiSchedulerParams schedParams = optimContext.scheduler.getParams();
    //        double t_P = schedParams.pickupDuration;
    //        double t_D = schedParams.dropoffDuration;
    //
    //        for (int i = 0; i < n; i++) {
    //            TaxiRequest iReq = rData.requests[i];
    //
    //            double t_i = pathTravelTimeCalc.calcTravelTime(iReq.getFromLink(), iReq.getToLink());
    //            double totalT_i = t_P + t_i + t_D;
    //
    //            for (int j = 0; j < n; j++) {
    //                TaxiRequest jReq = rData.requests[j];
    //                double t_ij = pathTravelTimeCalc.calcTravelTime(iReq.getToLink(),
    //                        jReq.getFromLink());
    //
    //                if (doExcludeReqToReqDrive(i, j, totalT_i, t_ij)) {
    //                    GRBLinExpr excludeX = new GRBLinExpr();
    //                    excludeX.addTerm(1, xVar[m + i][m + j]);
    //                    model.addConstr(excludeX, GRB.EQUAL, 0, "r2r excluded " + i + "," + j);
    //                }
    //                else {
    //                    GRBLinExpr expr = new GRBLinExpr();
    //                    expr.addTerm(1, wVar[j]);
    //                    expr.addTerm(-1, wVar[i]);
    //                    expr.addConstant(W_MAX);
    //                    expr.addTerm(-totalT_i - t_ij - W_MAX, xVar[m + i][m + j]);
    //                    model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(r2r)_" + i + "," + j);
    //                }
    //            }
    //        }
    //    }
    //
    //
    //    private boolean doExcludeReqToReqDrive(int i, int j, double totalT_i, double t_ij)
    //    {
    //        double e_i = rData.requests[i].getT0();
    //        double l_j = rData.requests[j].getT0() + TW_MAX;
    //        double earliestArrival_j = e_i + totalT_i + t_ij;
    //
    //        //e_i + t_P + t_i + t_D + t_ij > l_j ==> x[m+i][m+j] = 0
    //        if (earliestArrival_j > l_j) {
    //            return true;
    //        }
    //
    //        //t_ij > MAX_TP ==> x[m+i][m+j] = 0
    //        if (t_ij > TP_MAX) {
    //            return true;
    //        }
    //
    //        return false;
    //    }
    //
    //
    //    private void applyInitialSolution(MIPSolution initialSolution)
    //        throws GRBException
    //    {
    //        for (int u = 0; u < m + n; u++) {
    //            for (int v = 0; v < m + n; v++) {
    //                double x_uv = initialSolution.x[u][v] ? 1 : 0;
    //                xVar[u][v].set(GRB.DoubleAttr.Start, x_uv);
    //            }
    //        }
    //
    //        for (int i = 0; i < n; i++) {
    //            wVar[i].set(GRB.DoubleAttr.Start, initialSolution.w[i]);
    //        }
    //    }
    //
    //
    //    private MIPSolution extractSolution()
    //        throws GRBException
    //    {
    //        boolean[][] x = new boolean[m + n][m + n];
    //        for (int u = 0; u < m + n; u++) {
    //            for (int v = 0; v < m + n; v++) {
    //                double x_uv = xVar[u][v].get(GRB.DoubleAttr.X);
    //                x[u][v] = x_uv >= 0.5;
    //            }
    //        }
    //
    //        double[] w = new double[n];
    //        for (int i = 0; i < n; i++) {
    //            w[i] = wVar[i].get(GRB.DoubleAttr.X);
    //        }
    //
    //        return new MIPSolution(x, w);
    //    }
}
