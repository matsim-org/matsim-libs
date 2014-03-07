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

package playground.michalm.taxi.optimizer.gurobi;

import gurobi.*;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.utils.LeastCostPathTree.NodeData;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.util.TaxicabUtils;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class MIPProblem
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final LeastCostPathTreeStorage leastCostPathTrees;

    private Set<TaxiRequest> unplannedRequests;
    private TaxiRequest[] requests;
    private List<Vehicle> vehicles;

    private GRBModel model;

    private int n;//request count
    private int m;//vehicle count
    private GRBVar[][] xVar;//for each request/vehicle pair, (i, j)
    private GRBVar[] wVar; //for each request, i


    public MIPProblem(TaxiOptimizerConfiguration optimConfig,
            LeastCostPathTreeStorage leastCostPathTrees)
    {
        if (!optimConfig.scheduler.getParams().destinationKnown) {
            throw new IllegalArgumentException("Destinations must be known ahead");
        }

        this.optimConfig = optimConfig;
        this.leastCostPathTrees = leastCostPathTrees;
    }


    void scheduleUnplannedRequests(Set<TaxiRequest> unplannedRequests)
    {
        this.unplannedRequests = unplannedRequests;
        optimConfig.scheduler.removePlannedRequestsFromAllSchedules(unplannedRequests);

        n = unplannedRequests.size();
        if (n == 0) {
            return;
        }

        requests = unplannedRequests.toArray(new TaxiRequest[n]);
        vehicles = TaxicabUtils.getVehiclesAsList(optimConfig.context.getVrpData().getVehicles(),
                TaxicabUtils.CAN_BE_SCHEDULED);

        m = vehicles.size();
        if (m == 0) {
            return;
        }

        solveWithGurobi();
    }


    private void solveWithGurobi()
    {
        try {
            GRBEnv env = new GRBEnv();
            model = new GRBModel(env);

            addXVariables();
            addWVariables();
            model.update();

            setObjective();

            addOneIncomingConstraint();
            addOneOutgoingConstraint();

            addVehToReqLinConstraint();
            addReqToReqLinConstraint();

            model.optimize();

            updateSchedules();

            model.dispose();
            env.dispose();
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
            double e_i = requests[i].getT0();
            wVar[i] = model.addVar(e_i, GRB.INFINITY, 1, GRB.CONTINUOUS, "w_" + i);
        }
    }


    private void setObjective()
        throws GRBException
    {
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

            for (int k = 0; k < m; k++) {
                Vehicle veh = vehicles.get(k);
                LinkTimePair departure = optimConfig.scheduler.getEarliestIdleness(veh);
                Map<Id, NodeData> tree = leastCostPathTrees.getTree(departure.link);

                double a_k = departure.time;
                double t_O_ki = getTravelTime(tree, requests[i].getFromLink());
                expr.addTerm(-a_k - t_O_ki, xVar[k][m + i]);
            }

            model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(v2r)_" + i);
        }
    }


    private static final double T = 7 * 24 * 60 * 60;//one week


    private void addReqToReqLinConstraint()
        throws GRBException
    {
        TaxiSchedulerParams schedParams = optimConfig.scheduler.getParams();
        double t_P = schedParams.pickupDuration;
        double t_D = schedParams.dropoffDuration;

        for (int i = 0; i < n; i++) {
            TaxiRequest iReq = requests[i];

            Map<Id, NodeData> iTree = leastCostPathTrees.getTree(iReq.getFromLink());
            double t_i = getTravelTime(iTree, iReq.getToLink());
            double iTotalServeTime = t_P + t_i + t_D;

            Map<Id, NodeData> ijTree = leastCostPathTrees.getTree(iReq.getToLink());
            for (int j = 0; j < n; j++) {
                TaxiRequest jReq = requests[j];
                double t_ij = getTravelTime(ijTree, jReq.getFromLink());

                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, wVar[j]);
                expr.addTerm(-1, wVar[i]);
                expr.addConstant(T);
                expr.addTerm(-iTotalServeTime - t_ij - T, xVar[m + i][m + j]);
                model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(r2r)_" + i + "," + j);
            }
        }
    }


    private double getTravelTime(Map<Id, NodeData> tree, Link toLink)
    {
        double tt = 1;//getting over the first node
        tt += tree.get(toLink.getFromNode().getId()).getTime();//travelling along the path
        tt += toLink.getLength() / toLink.getFreespeed();//travelling the last link (approx.)
        return tt;
    }


    private Vehicle currentVeh;
    private double[][] x;
    private double[] w;


    private void updateSchedules()
        throws GRBException
    {
        x = model.get(GRB.DoubleAttr.X, xVar);
        w = model.get(GRB.DoubleAttr.X, wVar);

        for (int k = 0; k < m; k++) {
            currentVeh = vehicles.get(k);
            addSubsequentRequestsToCurrentVehicle(x[k]);
        }
    }


    private void addSubsequentRequestsToCurrentVehicle(double[] x_u)
    {
        for (int i = 0; i < n; i++) {
            if (x_u[m + i] > 0.5) {
                addRequestToCurrentVehicle(requests[i], w[i]);
                addSubsequentRequestsToCurrentVehicle(x[m + i]);
                return;
            }
        }
    }


    private static final double EPSILON = 0.01;


    private void addRequestToCurrentVehicle(TaxiRequest req, double w_i)
    {
        LinkTimePair earliestDeparture = optimConfig.scheduler.getEarliestIdleness(currentVeh);
        Link fromLink = earliestDeparture.link;

        double departureTime = w_i - getTravelTime(fromLink, req.getFromLink());

        double diff = earliestDeparture.time - departureTime;

        if (diff > EPSILON) {
            throw new IllegalStateException();
        }
        else if (diff > -EPSILON) {//use earliestDeparture.time
            departureTime = earliestDeparture.time;
        }

        VrpPathWithTravelData path = optimConfig.calculator.calcPath(fromLink, req.getFromLink(),
                departureTime);
        
        VehicleRequestPath vrPath = new VehicleRequestPath(currentVeh, req, path);
        optimConfig.scheduler.scheduleRequest(vrPath);
        unplannedRequests.remove(req);
    }


    private double getTravelTime(Link fromLink, Link toLink)
    {
        Map<Id, NodeData> tree = leastCostPathTrees.getTree(fromLink);
        return getTravelTime(tree, toLink);
    }
}
