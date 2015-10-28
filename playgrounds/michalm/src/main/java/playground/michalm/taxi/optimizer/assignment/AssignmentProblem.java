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

package playground.michalm.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.RequestFilter;


public class AssignmentProblem
{
    private static final double NULL_PATH_COST = 48 * 60 * 60; //2 days

    private final TaxiOptimizerConfiguration optimConfig;
    private final MultiNodeDijkstra router;
    private final RequestFilter requestFilter;

    private final double planningHorizon = 1800; //30 min TODO

    private VehicleData vData;
    private AssignmentRequestData rData;


    public AssignmentProblem(TaxiOptimizerConfiguration optimConfig, MultiNodeDijkstra router)
    {
        this.optimConfig = optimConfig;
        this.router = router;
        this.requestFilter = optimConfig.filterFactory.createRequestFilter();
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
        rData = new AssignmentRequestData(optimConfig, unplannedRequests, planningHorizon);
        if (rData.dimension == 0) {
            return false;
        }

        vData = new VehicleData(optimConfig, planningHorizon);
        return vData.dimension > 0;
    }


    private static class RequestPathData
    {
        private Node reqNode;//destination
        private double delay;//at the first and last links
        private Path path;//shortest path
    }


    private RequestPathData[][] createPathDataMatrix()
    {
        RequestPathData[][] pathDataMatrix = new RequestPathData[vData.entries.size()][];
        for (int v = 0; v < pathDataMatrix.length; v++) {
            pathDataMatrix[v] = calcPathsForVehicle(vData.entries.get(v));
        }
        return pathDataMatrix;
    }


    private RequestPathData[] calcPathsForVehicle(VehicleData.Entry departure)
    {
        Node fromNode = departure.link.getToNode();

        Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
        Map<Id<Node>, Path> pathsToReqNodes = new HashMap<>();
        RequestPathData[] pathDataArray = new RequestPathData[rData.dimension];

        Iterable<TaxiRequest> filteredReqs = requestFilter.filterRequestsForVehicle(rData.requests,
                departure.vehicle);

        for (TaxiRequest req : filteredReqs) {
            int r = rData.reqIdx.get(req.getId());
            RequestPathData pathData = pathDataArray[r] = new RequestPathData();
            Link reqLink = req.getFromLink();

            if (departure.link == reqLink) {
                //hack: we are basically there (on the same link), so let's pretend reqNode == fromNode
                pathData.reqNode = departure.link.getToNode();
                pathData.delay = 0;
            }
            else {
                pathData.reqNode = reqLink.getFromNode();
                //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                pathData.delay = 1 + reqLink.getFreespeed(departure.time);
            }

            if (!initialNodes.containsKey(pathData.reqNode.getId())) {
                InitialNode newInitialNode = new InitialNode(pathData.reqNode, 0, 0);
                initialNodes.put(pathData.reqNode.getId(), newInitialNode);
            }
        }

        ImaginaryNode toNodes = router.createImaginaryNode(initialNodes.values());
        Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time, null, null);
        Node bestReqNode = path.nodes.get(path.nodes.size() - 1);
        pathsToReqNodes.put(bestReqNode.getId(), path);

        //get paths for all remaining endNodes 
        for (InitialNode i : initialNodes.values()) {
            Node reqNode = i.node;
            if (reqNode.getId() != bestReqNode.getId()) {
                path = router.constructPath(fromNode, reqNode, departure.time);
                pathsToReqNodes.put(reqNode.getId(), path);
            }
        }

        for (TaxiRequest req : filteredReqs) {
            int r = rData.reqIdx.get(req.getId());
            RequestPathData pathData = pathDataArray[r];
            pathData.path = pathsToReqNodes.get(pathData.reqNode.getId());
        }

        return pathDataArray;
    }


    private double[][] createCostMatrix(RequestPathData[][] pathDataMatrix)
    {
        boolean reduceTP = doReduceTP();
        double[][] costMatrix = new double[vData.dimension][rData.dimension];

        for (int v = 0; v < vData.dimension; v++) {
            VehicleData.Entry departure = vData.entries.get(v);

            for (int r = 0; r < rData.dimension; r++) {
                RequestPathData pathData = pathDataMatrix[v][r];

                double travelTime = pathData == null ? //
                        NULL_PATH_COST : // no path (too far away)
                        pathData.delay + pathData.path.travelTime;

                double pickupBeginTime = Math.max(rData.requests.get(r).getT0(),
                        departure.time + travelTime);

                costMatrix[v][r] = reduceTP ? //
                        pickupBeginTime - departure.time : // T_P

                //TODO the following two variants, (A) and (B), behave different
                //when undersupply happens (i.e. with dummy vehicles)

                //(A) more fairness, lower throughput
                //Math.max(departure.time + travelTime - rData.requests.get(r).getT0(), 0); // T_W

                //(B)less fairness, higher throughput
                pickupBeginTime;// T_W
            }
        }

        return costMatrix;
    }


    private boolean doReduceTP()
    {
        switch (optimConfig.goal) {
            case MIN_PICKUP_TIME:
                return true;

            case MIN_WAIT_TIME:
                return false;

            case DEMAND_SUPPLY_EQUIL:
                return rData.urgentReqCount > vData.idleCount;

            default:
                throw new IllegalStateException();
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

            VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link, req.getFromLink(),
                    departure.time, pathData.path, optimConfig.travelTime);

            optimConfig.scheduler.scheduleRequest(departure.vehicle, req, vrpPath);
            unplannedRequests.remove(req);
        }
    }
}
