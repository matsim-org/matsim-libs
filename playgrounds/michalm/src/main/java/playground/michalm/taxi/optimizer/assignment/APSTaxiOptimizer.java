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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class APSTaxiOptimizer
    implements TaxiOptimizer
{
    private final TaxiOptimizerConfiguration optimConfig;

    private final SortedSet<TaxiRequest> unplannedRequests;

    private boolean requiresReoptimization = false;

    private VehicleData vData;
    private RequestData rData;


    public APSTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        unplannedRequests = new TreeSet<TaxiRequest>(Requests.T0_COMPARATOR);
    }


    @Override
    public TaxiOptimizerConfiguration getConfiguration()
    {
        return optimConfig;
    }


    private void removePlannedRequests()
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
            optimConfig.scheduler.removePlannedRequests(TaxiSchedules.getSchedule(veh),
                    unplannedRequests);
        }
    }


    private VrpPathWithTravelData[] createVrpPathsForRequest(TaxiRequest req)
    {
        VrpPathWithTravelData[] reqPaths = new VrpPathWithTravelData[vData.dimension];

        for (int v = 0; v < vData.dimension; v++) {
            LinkTimePair departure = vData.departures.get(v);
            reqPaths[v] = optimConfig.calculator.calcPath(departure.link, req.getFromLink(),
                    departure.time);
        }

        return reqPaths;
    }


    private List<VrpPathWithTravelData[]> createVrpPaths()
    {
        List<VrpPathWithTravelData[]> paths = new ArrayList<VrpPathWithTravelData[]>(
                rData.urgentReqCount);

        int rMin = Math.max(rData.urgentReqCount, vData.dimension);
        Max maxArrivalTimeForRMinRequests = new Max();

        for (int r = 0; r < rMin; r++) {
            TaxiRequest req = rData.requests[r];
            VrpPathWithTravelData[] reqPaths = createVrpPathsForRequest(req);
            paths.add(reqPaths);

            for (VrpPathWithTravelData path : reqPaths) {
                maxArrivalTimeForRMinRequests.increment(path.getArrivalTime());
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


    private boolean doReduceTP()
    {
        switch (optimConfig.goal) {
            case MIN_PICKUP_TIME:
                return true;

            case MIN_WAIT_TIME:
                return false;

            case DEMAND_SUPPLY_EQUIL:
                return rData.urgentReqCount > vData.idleVehCount;

            default:
                throw new IllegalStateException();
        }
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
                double pickupBeginTime = Math.max(req.getT0(), path.getArrivalTime());

                reqToVehCostMatrix[r][v] = reduceTP ? //
                        pickupBeginTime - vData.departures.get(v).time : //T_P
                        pickupBeginTime;//T_W
            }
        }

        return reqToVehCostMatrix;
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

            Vehicle veh = vData.vehicles.get(v);
            TaxiRequest req = rData.requests[r];
            optimConfig.scheduler.scheduleRequest(new VehicleRequestPath(veh, req, path));
            unplannedRequests.remove(req);
        }
    }


    protected void scheduleUnplannedRequests()
    {
        removePlannedRequests();

        RequestData rData = new RequestData(optimConfig, unplannedRequests);
        if (rData.dimension == 0) {
            return;
        }

        VehicleData vData = new VehicleData(optimConfig);
        if (vData.dimension == 0) {
            return;
        }

        List<VrpPathWithTravelData[]> pathsByReq = createVrpPaths();

        double[][] reqToVehCostMatrix = createReqToVehCostMatrix(pathsByReq);

        int[] reqToVehAssignments = new HungarianAlgorithm(reqToVehCostMatrix).execute();

        scheduleRequests(reqToVehAssignments, pathsByReq);
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization) {
            scheduleUnplannedRequests();
        }
    }


    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequests.add((TaxiRequest)request);
        requiresReoptimization = true;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;

        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);
        TaxiTask nextTask = taxiSchedule.nextTask();

        if (!optimConfig.scheduler.getParams().destinationKnown) {
            if (nextTask != null // schedule != COMPLETED
                    && nextTask.getTaxiTaskType() == TaxiTaskType.DROPOFF_DRIVE) {
                requiresReoptimization = true;
            }
        }
    }


    //TODO switch on/off
    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getTaskTracker().predictEndTime(
                optimConfig.context.getTime());
        optimConfig.scheduler.updateCurrentAndPlannedTasks(schedule, predictedEndTime);

        //we may here possibly decide here whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
