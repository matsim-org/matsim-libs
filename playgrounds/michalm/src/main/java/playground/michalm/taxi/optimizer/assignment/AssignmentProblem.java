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
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class AssignmentProblem
{
    private final double NULL_PATH_COST = 24 * 60 * 60; //1 day

    private final TaxiOptimizerConfiguration optimConfig;

    private SortedSet<TaxiRequest> unplannedRequests;
    private VehicleData vData;
    private AssignmentRequestData rData;


    public AssignmentProblem(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
    }


    public void scheduleUnplannedRequests(SortedSet<TaxiRequest> unplannedRequests)
    {
        this.unplannedRequests = unplannedRequests;

        initData();
        if (vData.dimension == 0 || rData.dimension == 0) {
            return;
        }

        List<VrpPathWithTravelData[]> pathsByReq = createVrpPaths();
        double[][] reqToVehCostMatrix = createReqToVehCostMatrix(pathsByReq);
        int[] reqToVehAssignments = new HungarianAlgorithm(reqToVehCostMatrix).execute();

        scheduleRequests(reqToVehAssignments, pathsByReq);
    }


    private void initData()
    {
        rData = new AssignmentRequestData(optimConfig, unplannedRequests);
        if (rData.dimension == 0) {
            return;
        }

        vData = new VehicleData(optimConfig);
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

        Max maxArrivalTimeForRMinRequests = new Max();

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
                return paths;
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
            reqPaths[v] = optimConfig.calculator.calcPath(departure.link, req.getFromLink(),
                    departure.time);
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
