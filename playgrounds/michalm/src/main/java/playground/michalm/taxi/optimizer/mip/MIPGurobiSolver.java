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

import gurobi.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.mip.MIPProblem.MIPSolution;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;


public class MIPGurobiSolver
{
    private static final double W_MAX = 30 * 60 * 60;//30 hours

    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;
    private final MIPRequestData rData;
    private final VehicleData vData;
    private final int n;//request count
    private final int m;//vehicle count

    private GRBModel model;
    private GRBVar[][] xVar;//for each request/vehicle pair, (i, j)
    private GRBVar[] wVar; //for each request, i


    MIPGurobiSolver(TaxiOptimizerConfiguration optimConfig,
            PathTreeBasedTravelTimeCalculator pathTravelTimeCalc, MIPRequestData rData,
            VehicleData vData)
    {
        this.optimConfig = optimConfig;
        this.pathTravelTimeCalc = pathTravelTimeCalc;
        this.rData = rData;
        this.vData = vData;

        n = rData.dimension;
        m = vData.dimension;
    }


    MIPSolution solve(MIPSolution initialSolution)
    {
        try {
            model = new GRBModel(new GRBEnv());

            // this is the internal model (a copy of that passed to the constructor)
            GRBEnv env = model.getEnv();

            //env.set(DoubleParam.TimeLimit, 3600);// 1 hour
            //env.set(GRB.DoubleParam.MIPGap, 0.01);//1%
            
            env.set(GRB.IntParam.MIPFocus, 1);//the focus towards finding feasible solutions
            //or alternatively: focus towards finding feasible solutions after 1 hour
            //env.set(GRB.DoubleParam.ImproveStartTime, 3600);

            addXVariables();
            addWVariables();
            model.update();

            setObjective();

            addOneIncomingConstraint();
            addOneOutgoingConstraint();

            addVehToReqLinConstraint();
            addReqToReqLinConstraint();
            model.update();

            //model.write("D:\\model.lp");

            applyInitialSolution(initialSolution);

            model.optimize();

            MIPSolution solution = extractSolution();

            model.dispose();
            env.dispose();

            return solution;
        }
        catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }


    private void addXVariables()
        throws GRBException
    {
        xVar = new GRBVar[m + n][m + n];
        for (int u = 0; u < m + n; u++) {
            for (int v = 0; v < m + n; v++) {
                xVar[u][v] = model.addVar(0, 1, 0, GRB.BINARY, "x_" + u + "," + v);
            }
        }
    }


    private void addWVariables()
        throws GRBException
    {
        wVar = new GRBVar[n];
        for (int i = 0; i < n; i++) {
            double e_i = rData.requests[i].getT0();
            wVar[i] = model.addVar(e_i, W_MAX, 0, GRB.CONTINUOUS, "w_" + i);
        }
    }


    private void setObjective()
        throws GRBException
    {
        GRBLinExpr obj = new GRBLinExpr();
        for (int i = 0; i < n; i++) {
            obj.addTerm(1, wVar[i]);

            double e_i = rData.requests[i].getT0();
            obj.addConstant(-e_i);
        }

        model.setObjective(obj);
        model.set(GRB.IntAttr.ModelSense, 1);
    }


    private void addOneIncomingConstraint()
        throws GRBException
    {
        for (int v = 0; v < m + n; v++) {
            GRBLinExpr expr = new GRBLinExpr();

            for (int u = 0; u < m + n; u++) {
                expr.addTerm(1, xVar[u][v]);
            }

            model.addConstr(expr, GRB.EQUAL, 1, "incoming: x_u," + v);
        }
    }


    private void addOneOutgoingConstraint()
        throws GRBException
    {
        for (int u = 0; u < m + n; u++) {
            GRBLinExpr expr = new GRBLinExpr();

            for (int v = 0; v < m + n; v++) {
                expr.addTerm(1, xVar[u][v]);
            }

            model.addConstr(expr, GRB.EQUAL, 1, "outgoing: x_" + u + ",v");
        }
    }


    private void addVehToReqLinConstraint()
        throws GRBException
    {
        for (int i = 0; i < n; i++) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1, wVar[i]);

            Link toLink = rData.requests[i].getFromLink();

            for (int k = 0; k < m; k++) {
                Vehicle veh = vData.vehicles.get(k);
                LinkTimePair departure = optimConfig.scheduler.getEarliestIdleness(veh);

                double a_k = departure.time;
                double t_O_ki = pathTravelTimeCalc.calcTravelTime(departure.link, toLink);
                expr.addTerm(-a_k - t_O_ki, xVar[k][m + i]);
            }

            model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(v2r)_" + i);
        }
    }


    private void addReqToReqLinConstraint()
        throws GRBException
    {
        TaxiSchedulerParams schedParams = optimConfig.scheduler.getParams();
        double t_P = schedParams.pickupDuration;
        double t_D = schedParams.dropoffDuration;

        for (int i = 0; i < n; i++) {
            TaxiRequest iReq = rData.requests[i];

            double t_i = pathTravelTimeCalc.calcTravelTime(iReq.getFromLink(), iReq.getToLink());
            double iTotalServeTime = t_P + t_i + t_D;

            for (int j = 0; j < n; j++) {
                TaxiRequest jReq = rData.requests[j];
                double t_ij = pathTravelTimeCalc.calcTravelTime(iReq.getToLink(),
                        jReq.getFromLink());

                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, wVar[j]);
                expr.addTerm(-1, wVar[i]);
                expr.addConstant(W_MAX);
                expr.addTerm(-iTotalServeTime - t_ij - W_MAX, xVar[m + i][m + j]);
                model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(r2r)_" + i + "," + j);
            }
        }
    }


    private void applyInitialSolution(MIPSolution initialSolution)
        throws GRBException
    {
        for (int u = 0; u < m + n; u++) {
            for (int v = 0; v < m + n; v++) {
                xVar[u][v].set(GRB.DoubleAttr.Start, initialSolution.x[u][v]);
            }
        }

        for (int i = 0; i < n; i++) {
            wVar[i].set(GRB.DoubleAttr.Start, initialSolution.w[i]);
        }

        //the following shortcut does not work (why??):
        //model.set(GRB.DoubleAttr.Start, wVar, initialSolution.w);
        //model.set(GRB.DoubleAttr.Start, xVar, initialSolution.x);
    }


    private MIPSolution extractSolution()
        throws GRBException
    {
        return new MIPSolution(//
                model.get(GRB.DoubleAttr.X, xVar),//
                model.get(GRB.DoubleAttr.X, wVar));
    }
}
