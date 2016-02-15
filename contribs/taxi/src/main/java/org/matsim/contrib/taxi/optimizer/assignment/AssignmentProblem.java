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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.lang.reflect.Array;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.filter.*;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.google.common.collect.Iterables;


public class AssignmentProblem
{
    private final TaxiOptimizerContext optimContext;
    private final AssignmentTaxiOptimizerParams params;

    private final MultiNodeDijkstra router;
    private final BackwardFastMultiNodeDijkstra backwardRouter;

    private final KStraightLineNearestRequestFilter requestFilter;
    private final KStraightLineNearestVehicleDepartureFilter vehicleFilter;

    private VehicleData vData;
    private AssignmentRequestData rData;


    public AssignmentProblem(TaxiOptimizerContext optimContext, MultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter)
    {
        this.optimContext = optimContext;
        this.params = (AssignmentTaxiOptimizerParams)optimContext.optimizerParams;
        this.router = router;
        this.requestFilter = new KStraightLineNearestRequestFilter(optimContext.scheduler,
                params.nearestRequestsLimit);
        this.vehicleFilter = new KStraightLineNearestVehicleDepartureFilter(
                params.nearestVehiclesLimit);

        this.backwardRouter = backwardRouter;
    }


    public void scheduleUnplannedRequests(SortedSet<TaxiRequest> unplannedRequests)
    {
        if (initDataAndCheckIfSchedulingRequired(unplannedRequests)) {
            RequestPathData[][] pathDataMatrix = createPathDataMatrix();
            double[][] costMatrix = createCostMatrix(pathDataMatrix);
            int[] assignments = new HungarianAlgorithm(costMatrix).execute();
            scheduleRequests(assignments, pathDataMatrix, unplannedRequests);
        }
    }


    private boolean initDataAndCheckIfSchedulingRequired(SortedSet<TaxiRequest> unplannedRequests)
    {
        rData = new AssignmentRequestData(optimContext, unplannedRequests, 0);//only immediate reqs
        if (rData.dimension == 0) {
            return false;
        }

        int idleVehs = Iterables
                .size(Iterables.filter(optimContext.context.getVrpData().getVehicles().values(),
                        TaxiSchedulerUtils.createIsIdle(optimContext.scheduler)));

        if (idleVehs < rData.urgentReqCount) {
            vData = new VehicleData(optimContext, params.vehPlanningHorizonUndersupply);
        }
        else {
            vData = new VehicleData(optimContext, params.vehPlanningHorizonOversupply);
        }

        return vData.dimension > 0;
    }


    private static class RequestPathData
    {
        private Node node;//destination
        private double delay;//at the first and last links
        private Path path;//shortest path
    }


    private static int calcPathsForVehiclesCount = 0;
    private static int calcPathsForRequestsCount = 0;


    private RequestPathData[][] createPathDataMatrix()
    {
        RequestPathData[][] pathDataMatrix = (RequestPathData[][])Array
                .newInstance(RequestPathData.class, vData.dimension, rData.dimension);

        if (rData.dimension > vData.dimension) {
            calcPathsForVehicles(pathDataMatrix);
            calcPathsForVehiclesCount++;
        }
        else {
            calcPathsForRequests(pathDataMatrix);
            calcPathsForRequestsCount++;
        }

        if ( (calcPathsForRequestsCount + calcPathsForVehiclesCount) % 100 == 0) {
            System.err.println("PathsForRequests = " + calcPathsForRequestsCount
                    + " PathsForVehicles = " + calcPathsForVehiclesCount);
            System.err.println("reqs = " + rData.dimension + " vehs = " + vData.dimension
                    + " idleVehs = " + vData.idleCount);
        }

        return pathDataMatrix;
    }


    private void calcPathsForVehicles(RequestPathData[][] pathDataMatrix)
    {
        for (int v = 0; v < vData.dimension; v++) {
            VehicleData.Entry departure = vData.entries.get(v);
            Node fromNode = departure.link.getToNode();

            Map<Id<Node>, InitialNode> reqInitialNodes = new HashMap<>();
            Map<Id<Node>, Path> pathsToReqNodes = new HashMap<>();

            Iterable<TaxiRequest> filteredReqs = requestFilter
                    .filterRequestsForVehicle(rData.requests, departure.vehicle);

            for (TaxiRequest req : filteredReqs) {
                int r = rData.reqIdx.get(req.getId());
                RequestPathData pathData = pathDataMatrix[v][r] = new RequestPathData();
                Link reqLink = req.getFromLink();

                if (departure.link == reqLink) {
                    //hack: we are basically there (on the same link), so let's pretend reqNode == fromNode
                    pathData.node = fromNode;
                    pathData.delay = 0;
                }
                else {
                    pathData.node = reqLink.getFromNode();
                    //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                    pathData.delay = 1 + reqLink.getFreespeed(departure.time);
                }

                if (!reqInitialNodes.containsKey(pathData.node.getId())) {
                    InitialNode newInitialNode = new InitialNode(pathData.node, 0, 0);
                    reqInitialNodes.put(pathData.node.getId(), newInitialNode);
                }
            }

            ImaginaryNode toNodes = router.createImaginaryNode(reqInitialNodes.values());
            Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time, null, null);
            Node bestReqNode = path.nodes.get(path.nodes.size() - 1);
            pathsToReqNodes.put(bestReqNode.getId(), path);

            //get paths for all remaining endNodes 
            for (InitialNode i : reqInitialNodes.values()) {
                Node reqNode = i.node;
                if (reqNode.getId() != bestReqNode.getId()) {
                    path = router.constructPath(fromNode, reqNode, departure.time);
                    pathsToReqNodes.put(reqNode.getId(), path);
                }
            }

            for (TaxiRequest req : filteredReqs) {
                int r = rData.reqIdx.get(req.getId());
                RequestPathData pathData = pathDataMatrix[v][r];
                pathData.path = pathsToReqNodes.get(pathData.node.getId());
            }
        }
    }


    //TODO does not support adv reqs
    private void calcPathsForRequests(RequestPathData[][] pathDataMatrix)
    {
        double currTime = optimContext.context.getTime();

        for (int r = 0; r < rData.dimension; r++) {
            TaxiRequest req = rData.requests.get(r);
            Link toLink = req.getFromLink();
            Node toNode = toLink.getFromNode();

            Map<Id<Node>, InitialNode> vehInitialNodes = new HashMap<>();
            Map<Id<Node>, Path> pathsFromVehNodes = new HashMap<>();

            Iterable<VehicleData.Entry> filteredVehs = vehicleFilter
                    .filterVehiclesForRequest(vData.entries, req);

            for (VehicleData.Entry departure : filteredVehs) {
                int v = departure.idx;
                RequestPathData pathData = pathDataMatrix[v][r] = new RequestPathData();

                if (departure.link == toLink) {
                    //hack: we are basically there (on the same link), so let's pretend vehNode == toNode
                    pathData.node = toNode;
                    pathData.delay = 0;
                }
                else {
                    pathData.node = departure.link.getToNode();
                    //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                    pathData.delay = 1 + toLink.getFreespeed(departure.time);
                }

                if (!vehInitialNodes.containsKey(pathData.node.getId())) {
                    InitialNode newInitialNode = new InitialNode(pathData.node, 0, 0);
                    vehInitialNodes.put(pathData.node.getId(), newInitialNode);
                }
            }

            ImaginaryNode fromNodes = backwardRouter.createImaginaryNode(vehInitialNodes.values());
            Path path = backwardRouter.calcLeastCostPath(toNode, fromNodes, currTime, null, null);
            Node bestVehNode = path.nodes.get(path.nodes.size() - 1);
            pathsFromVehNodes.put(bestVehNode.getId(), path);

            //get paths for all remaining endNodes 
            for (InitialNode i : vehInitialNodes.values()) {
                Node vehNode = i.node;
                if (vehNode.getId() != bestVehNode.getId()) {
                    path = backwardRouter.constructPath(toNode, vehNode, currTime);
                    pathsFromVehNodes.put(vehNode.getId(), path);
                }
            }

            for (VehicleData.Entry departure : filteredVehs) {
                int v = departure.idx;
                RequestPathData pathData = pathDataMatrix[v][r];
                pathData.path = pathsFromVehNodes.get(pathData.node.getId());
            }
        }
    }


    public enum Mode
    {
        PICKUP_TIME, //
        //TTki

        ARRIVAL_TIME, //
        //DEPk + TTki
        //equivalent to REMAINING_WAIT_TIME, i.e. DEPk + TTki - Tcurr // TODO check this out for dummy cases

        TOTAL_WAIT_TIME, //
        //DEPk + TTki - T0i

        DSE;//
        //balance between demand (ARRIVAL_TIME) and supply (PICKUP_TIME)
    };


    private double[][] createCostMatrix(RequestPathData[][] pathDataMatrix)
    {
        Mode currentMode = getCurrentMode();
        double[][] costMatrix = new double[vData.dimension][rData.dimension];

        for (int v = 0; v < vData.dimension; v++) {
            VehicleData.Entry departure = vData.entries.get(v);
            for (int r = 0; r < rData.dimension; r++) {
                costMatrix[v][r] = calcCost(departure, rData.requests.get(r), pathDataMatrix[v][r],
                        currentMode);
            }
        }

        return costMatrix;
    }


    private double calcCost(VehicleData.Entry departure, TaxiRequest request,
            RequestPathData pathData, Mode currentMode)
    {
        double travelTime = pathData == null ? //
                params.nullPathCost : // no path (too far away)
                pathData.delay + pathData.path.travelTime;

        double pickupBeginTime = Math.max(request.getT0(), departure.time + travelTime);

        switch (currentMode) {
            case PICKUP_TIME:
                //this will work different than ARRIVAL_TIME at oversupply -> will reduce T_P and fairness
                return pickupBeginTime - departure.time;

            case ARRIVAL_TIME:
                //less fairness, higher throughput
                return pickupBeginTime;

            case TOTAL_WAIT_TIME:
                //more fairness, lower throughput
                //this will work different than than ARRIVAL_TIME at undersupply -> will reduce unfairness and throughput 
                return pickupBeginTime - request.getT0();

            default:
                throw new IllegalStateException();
        }
    }


    private Mode getCurrentMode()
    {
        if (params.mode != Mode.DSE) {
            return params.mode;
        }
        else {
            return rData.urgentReqCount > vData.idleCount ? Mode.PICKUP_TIME : //we have too few vehicles
                    Mode.ARRIVAL_TIME; //we have too many vehicles
        }
    }


    private void scheduleRequests(int[] assignments, RequestPathData[][] pathDataMatrix,
            SortedSet<TaxiRequest> unplannedRequests)
    {
        for (int v = 0; v < assignments.length; v++) {
            int r = assignments[v];

            if (r == -1 || //no request assigned
                    r >= rData.dimension) {// non-existing (dummy) request assigned
                continue;
            }

            VehicleData.Entry departure = vData.entries.get(v);
            TaxiRequest req = rData.requests.get(r);
            RequestPathData pathData = pathDataMatrix[v][r];

            VrpPathWithTravelData vrpPath = pathData == null ? //
                    VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(), departure.time,
                            router, optimContext.travelTime)
                    : VrpPaths.createPath(departure.link, req.getFromLink(), departure.time,
                            pathData.path, optimContext.travelTime);

            optimContext.scheduler.scheduleRequest(departure.vehicle, req, vrpPath);
            unplannedRequests.remove(req);
        }
    }
}
