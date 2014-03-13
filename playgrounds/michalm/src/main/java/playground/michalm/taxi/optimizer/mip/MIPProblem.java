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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.data.*;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.fifo.FIFOSchedulingProblem;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.util.TaxicabUtils;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;

import com.google.common.collect.*;


public class MIPProblem
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private SortedSet<TaxiRequest> unplannedRequests;
    private MIPRequestData rData;
    private List<Vehicle> vehicles;

    private GRBModel model;

    private int n;//request count
    private int m;//vehicle count
    private GRBVar[][] xVar;//for each request/vehicle pair, (i, j)
    private GRBVar[] wVar; //for each request, i


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
        List<TaxiRequest> removedRequests = optimConfig.scheduler
                .removePlannedRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);

        this.unplannedRequests = unplannedRequests;

        this.rData = new MIPRequestData(optimConfig, unplannedRequests);
        n = rData.dimension;
        if (n == 0) {
            return;
        }

        List<Vehicle> allVehs = optimConfig.context.getVrpData().getVehicles();
        Iterable<Vehicle> filteredVehs = Iterables.filter(allVehs,
                TaxicabUtils.createCanBeScheduled(optimConfig.scheduler));
        vehicles = Lists.newArrayList(filteredVehs);

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
            model.update();

            findInitialSolution();

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
            TaxiRequest req = rData.requests[i];
            double e_i = req.getT0();
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

            Link toLink = rData.requests[i].getFromLink();

            for (int k = 0; k < m; k++) {
                Vehicle veh = vehicles.get(k);
                LinkTimePair departure = optimConfig.scheduler.getEarliestIdleness(veh);

                double a_k = departure.time;
                double t_O_ki = pathTravelTimeCalc.calcTravelTime(departure.link, toLink);
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
                expr.addConstant(T);
                expr.addTerm(-iTotalServeTime - t_ij - T, xVar[m + i][m + j]);
                model.addConstr(expr, GRB.GREATER_EQUAL, 0, "w(r2r)_" + i + "," + j);
            }
        }
    }


    private void findInitialSolution()
        throws GRBException
    {
        Queue<TaxiRequest> queue = new PriorityQueue<TaxiRequest>(rData.dimension,
                Requests.T0_COMPARATOR);

        for (TaxiRequest r : rData.requests) {
            queue.add(r);
        }

        new FIFOSchedulingProblem(optimConfig).scheduleUnplannedRequests(queue);

        //schedules --> x[u][v] and w[i]

        for (int u = 0; u < m + n; u++) {
            for (int v = 0; v < m + n; v++) {
                xVar[u][v].set(GRB.DoubleAttr.Start, 0);
            }
        }

        double t_P = optimConfig.scheduler.getParams().pickupDuration;

        for (int k = 0; k < m; k++) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(vehicles.get(k));
            Iterable<TaxiRequest> reqs = TaxiSchedules.getTaxiRequests(schedule);
            Iterable<TaxiRequest> plannedReqs = Iterables.filter(reqs, TaxiRequests.IS_PLANNED);

            int u = k;
            for (TaxiRequest r : plannedReqs) {
                int i = rData.reqIdToIdx.get(r.getId());
                int v = m + i;

                xVar[u][v].set(GRB.DoubleAttr.Start, 1);

                double w_i = r.getPickupStayTask().getEndTime() - t_P;
                wVar[i].set(GRB.DoubleAttr.Start, w_i);

                u = v;
            }

            xVar[u][k].set(GRB.DoubleAttr.Start, 1);
        }

        optimConfig.scheduler.removePlannedRequestsFromAllSchedules();
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
                addRequestToCurrentVehicle(rData.requests[i], w[i]);
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

        double latestDepartureTime = w_i
                - pathTravelTimeCalc.calcTravelTime(earliestDeparture.link, req.getFromLink());

        double diff = latestDepartureTime - earliestDeparture.time;
        if (diff < -EPSILON) {
            throw new IllegalStateException();
        }

        VrpPathWithTravelData path = optimConfig.calculator.calcPath(fromLink, req.getFromLink(),
                earliestDeparture.time);

        VehicleRequestPath vrPath = new VehicleRequestPath(currentVeh, req, path);
        optimConfig.scheduler.scheduleRequest(vrPath);
        unplannedRequests.remove(req);
    }
}
