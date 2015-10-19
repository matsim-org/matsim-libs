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

import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.DijkstraWithThinPath;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class AssignmentProblem
{
    private final double NULL_PATH_COST = 48 * 60 * 60; //2 days

    private final TaxiOptimizerConfiguration optimConfig;
    private final LeastCostPathCalculator router;

    private SortedSet<TaxiRequest> unplannedRequests;
    private VehicleData vData;
    private AssignmentRequestData rData;

    public AssignmentProblem(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        router = new DijkstraWithThinPath(optimConfig.context.getScenario().getNetwork(),
                optimConfig.travelDisutility, optimConfig.travelTime);
    }


    public void scheduleUnplannedRequests(SortedSet<TaxiRequest> unplannedRequests)
    {
        this.unplannedRequests = unplannedRequests;

        if (!initDataAndCheckIfSchedulingRequired()) {
            return;
        }

        List<VrpPathWithTravelData[]> pathsByReq = createVrpPaths();
        double[][] reqToVehCostMatrix = createReqToVehCostMatrix(pathsByReq);
        int[] reqToVehAssignments = new HungarianAlgorithm(reqToVehCostMatrix).execute();

        scheduleRequests(reqToVehAssignments, pathsByReq);
    }


    private boolean initDataAndCheckIfSchedulingRequired()
    {
        rData = new AssignmentRequestData(optimConfig, unplannedRequests);
        if (rData.dimension == 0) {
            return false;
        }

        vData = new VehicleData(optimConfig);
        return vData.dimension > 0;
    }


    private List<VrpPathWithTravelData[]> createVrpPaths()
    {
        List<VrpPathWithTravelData[]> paths = new ArrayList<>(rData.urgentReqCount);

        //if only imm reqs then rMin = rData.urgentReqCount = rData.dimension
        //if both imm+adv then rMin should be urgentReqCount + soonUrgentReqCount

        int rMin = rData.urgentReqCount;//include also "soonUrgentReqCount" if "adv" reqs
        if (rMin < vData.dimension) {
            rMin = Math.min(rData.dimension, vData.dimension);
        }

        Max maxArrivalTimeForRMinRequests = new Max();//heuristics

        for (int r = 0; r < rMin; r++) {
            TaxiRequest req = rData.requests[r];
            VrpPathWithTravelData[] reqPaths = createVrpPathsForRequest(req);
            paths.add(reqPaths);

            for (VrpPathWithTravelData path : reqPaths) {
                if (path != null) {
                    maxArrivalTimeForRMinRequests.increment(path.getArrivalTime());
                }
            }
        }

        for (int r = rMin; r < rData.dimension; r++) {
            TaxiRequest req = rData.requests[r];
            if (req.getT0() > maxArrivalTimeForRMinRequests.getResult()) {
                break;
            }

            paths.add(createVrpPathsForRequest(req));
        }

        return paths;
    }


    private VrpPathWithTravelData[] createVrpPathsForRequest(TaxiRequest req)
    {
        VrpPathWithTravelData[] reqPaths = new VrpPathWithTravelData[vData.dimension];

        for (int v = 0; v < vData.dimension; v++) {
            VehicleData.Entry departure = getVehicleDataEntry(v);
            reqPaths[v] = VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(),
                    departure.time, router, optimConfig.travelTime, optimConfig.travelDisutility);
        }

        return reqPaths;
    }


    private double[][] createReqToVehCostMatrix(List<VrpPathWithTravelData[]> pathsByReq)
    {
        boolean reduceTP = doReduceTP();
        double[][] reqToVehCostMatrix = new double[pathsByReq.size()][vData.dimension];

        for (int r = 0; r < reqToVehCostMatrix.length; r++) {
            TaxiRequest req = rData.requests[r];
            VrpPathWithTravelData[] reqPaths = pathsByReq.get(r);

            for (int v = 0; v < vData.dimension; v++) {
                VrpPathWithTravelData path = reqPaths[v];

                if (path == null) {
                    reqToVehCostMatrix[r][v] = NULL_PATH_COST;
                }
                else {
                    double pickupBeginTime = Math.max(req.getT0(), path.getArrivalTime());

                    reqToVehCostMatrix[r][v] = reduceTP ? //
                            pickupBeginTime - getVehicleDataEntry(v).time : //T_P
                            pickupBeginTime;//T_W
                }
            }
        }

        return reqToVehCostMatrix;
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


    private void scheduleRequests(int[] reqToVehAssignments,
            List<VrpPathWithTravelData[]> pathsByReq)
    {
        for (int r = 0; r < reqToVehAssignments.length; r++) {
            int v = reqToVehAssignments[r];

            if (v == -1 || //no vehicle assigned
                    v >= vData.dimension) {// non-existing vehicle assigned
                continue;
            }

            VrpPathWithTravelData path = pathsByReq.get(r)[v];

            Vehicle veh = getVehicleDataEntry(v).vehicle;
            TaxiRequest req = rData.requests[r];
            optimConfig.scheduler.scheduleRequest(new VehicleRequestPath(veh, req, path));
            unplannedRequests.remove(req);
        }
    }


    private VehicleData.Entry getVehicleDataEntry(int idx)
    {
        return vData.entries.get(idx);
    }
}
