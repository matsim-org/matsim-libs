/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.fifo;

import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource.STRAIGHT_LINE;

import java.util.*;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.schedule.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.query.*;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.vehreqpath.*;


public class NOSTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final VehicleFilter vehicleFilter;
    private final RequestFilter requestFilter;


    public static NOSTaxiOptimizer createNOS(TaxiOptimizerConfiguration optimConfig,
            TravelDisutilitySource tdisSource)
    {
        return tdisSource == STRAIGHT_LINE ? //
                NOSTaxiOptimizer.createNOSWithStraightLineDistance(optimConfig) : //
                NOSTaxiOptimizer.createNOSWithoutStraightLineDistance(optimConfig);
    }


    public static NOSTaxiOptimizer createNOSWithStraightLineDistance(
            TaxiOptimizerConfiguration optimConfig)
    {
        VehicleFilter vehFilter = new StraightLineNearestVehicleFinder(optimConfig.scheduler);
        RequestFilter reqFilter = new StraightLineNearestRequestFinder(optimConfig.scheduler);

        return new NOSTaxiOptimizer(optimConfig, vehFilter, reqFilter);
    }


    public static NOSTaxiOptimizer createNOSWithoutStraightLineDistance(
            TaxiOptimizerConfiguration optimConfig)
    {
        VehicleFilter vehFilter = VehicleFilter.NO_FILTER;
        RequestFilter reqFilter = RequestFilter.NO_FILTER;

        return new NOSTaxiOptimizer(optimConfig, vehFilter, reqFilter);
    }


    public NOSTaxiOptimizer(TaxiOptimizerConfiguration optimConfig, VehicleFilter vehicleFilter,
            RequestFilter requestFilter)
    {
        super(optimConfig, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR));
        this.vehicleFilter = vehicleFilter;
        this.requestFilter = requestFilter;
    }


    //==============================

    private Set<Vehicle> idleVehicles;


    @Override
    protected void scheduleUnplannedRequests()
    {
        initIdleVehicles();

        if (doReduceTP()) {
            scheduleIdleVehiclesImpl();//reduce T_P to increase throughput (demand > supply)
        }
        else {
            scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS)
        }
    }


    private void initIdleVehicles()
    {
        idleVehicles = new HashSet<Vehicle>();

        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
            if (optimConfig.scheduler.isIdle(veh)) {
                idleVehicles.add(veh);
            }
        }
    }


    private boolean doReduceTP()
    {
        switch (optimConfig.goal) {
            case MIN_PICKUP_TIME:
                return true;

            case MIN_WAIT_TIME:
                return false;

            case DEMAND_SUPPLY_EQUIL:
                int awaitingReqCount = Requests.countRequests(unplannedRequests,
                        new Requests.IsUrgentPredicate(optimConfig.context.getTime()));

                return awaitingReqCount > idleVehicles.size();

            default:
                throw new IllegalStateException();
        }
    }


    private void scheduleUnplannedRequestsImpl()
    {
        Queue<TaxiRequest> queue = (Queue<TaxiRequest>)unplannedRequests;

        while (!queue.isEmpty()) {
            if (idleVehicles.size() == 0) {
                return;
            }

            TaxiRequest req = queue.peek();

            Iterable<Vehicle> filteredVehs = vehicleFilter.filterVehiclesForRequest(idleVehicles,
                    req);

            VehicleRequestPath best = optimConfig.vrpFinder.findBestVehicleForRequest(req,
                    filteredVehs, VehicleRequestPaths.TW_COST);

            if (best == null) {
                continue;//either no idle vehicles or no "acceptable" path found
            }

            optimConfig.scheduler.scheduleRequest(best);
            queue.poll();
            idleVehicles.remove(best.vehicle);
        }
    }


    private void scheduleIdleVehiclesImpl()
    {
        for (Vehicle veh : idleVehicles) {
            if (unplannedRequests.size() == 0) {
                return;
            }

            Iterable<TaxiRequest> filteredReqs = requestFilter.filterRequestsForVehicle(
                    unplannedRequests, veh);

            VehicleRequestPath best = optimConfig.vrpFinder.findBestRequestForVehicle(veh,
                    filteredReqs, VehicleRequestPaths.TP_COST);

            if (best == null) {
                continue;//either no unplanned requests or no "acceptable" path found
            }

            optimConfig.scheduler.scheduleRequest(best);
            unplannedRequests.remove(best.request);
        }
    }


    //==============================

    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;

        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);
        taxiSchedule.nextTask();
    };


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {}
}
