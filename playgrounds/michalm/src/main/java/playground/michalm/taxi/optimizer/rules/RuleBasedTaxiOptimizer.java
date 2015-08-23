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

package playground.michalm.taxi.optimizer.rules;

import java.util.*;

import org.matsim.contrib.dvrp.data.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.vehreqpath.*;


public class RuleBasedTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final VehicleFilter vehicleFilter;
    private final RequestFilter requestFilter;
    private Set<Vehicle> idleVehicles;


    public RuleBasedTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), false);

        if (optimConfig.scheduler.getParams().vehicleDiversion) {
            throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
        }

        this.vehicleFilter = optimConfig.filterFactory.createVehicleFilter();
        this.requestFilter = optimConfig.filterFactory.createRequestFilter();
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        initIdleVehicles();

        if (isReduceTP()) {
            scheduleIdleVehiclesImpl();//reduce T_P to increase throughput (demand > supply)
        }
        else {
            scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS)
        }
    }


    private void initIdleVehicles()
    {
        idleVehicles = new LinkedHashSet<>();
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles().values()) {
            if (optimConfig.scheduler.isIdle(veh)) {
                idleVehicles.add(veh);
            }
        }
    }


    private boolean isReduceTP()
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


    //request-initiated scheduling
    private void scheduleUnplannedRequestsImpl()
    {
        Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
        while (reqIter.hasNext() && !idleVehicles.isEmpty()) {
            TaxiRequest req = reqIter.next();

            Iterable<Vehicle> filteredVehs = vehicleFilter.filterVehiclesForRequest(idleVehicles,
                    req);
            VehicleRequestPath best = optimConfig.vrpFinder.findBestVehicleForRequest(req,
                    filteredVehs, VehicleRequestPaths.TW_COST);

            if (best != null) {
                optimConfig.scheduler.scheduleRequest(best);
                reqIter.remove();
                idleVehicles.remove(best.vehicle);
            }
        }
    }


    //vehicle-initiated scheduling
    private void scheduleIdleVehiclesImpl()
    {
        Iterator<Vehicle> vehIter = idleVehicles.iterator();
        while (vehIter.hasNext() && !unplannedRequests.isEmpty()) {
            Vehicle veh = vehIter.next();

            Iterable<TaxiRequest> filteredReqs = requestFilter
                    .filterRequestsForVehicle(unplannedRequests, veh);
            VehicleRequestPath best = optimConfig.vrpFinder.findBestRequestForVehicle(veh,
                    filteredReqs, VehicleRequestPaths.TP_COST);

            if (best != null) {
                optimConfig.scheduler.scheduleRequest(best);
                unplannedRequests.remove(best.request);
            }
        }
    }


    @Override
    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return newCurrentTask.getTaxiTaskType() == TaxiTaskType.STAY;
    }
}
