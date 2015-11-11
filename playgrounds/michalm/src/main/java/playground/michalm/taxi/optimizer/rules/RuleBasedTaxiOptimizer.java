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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.zone.util.SquareGridSystem;


public class RuleBasedTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    protected final BestDispatchFinder dispatchFinder;
    private final VehicleFilter vehicleFilter;
    private final RequestFilter requestFilter;

    private IdleTaxiZonalRegistry idleTaxiRegistry;
    private UnplannedRequestZonalRegistry unplannedRequestRegistry;

    private final double cellSize = 1000;//1km//TODO 


    public RuleBasedTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), false);

        if (optimConfig.scheduler.getParams().vehicleDiversion) {
            throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
        }

        dispatchFinder = new BestDispatchFinder(optimConfig);

        Network network = optimConfig.context.getScenario().getNetwork();
        SquareGridSystem zonalSystem = SquareGridSystem.createSquareGridSystem(network, cellSize);
        idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem);
        unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);

        vehicleFilter = optimConfig.filterFactory.createVehicleFilter();
        requestFilter = optimConfig.filterFactory.createRequestFilter();
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        if (isReduceTP()) {
            scheduleIdleVehiclesImpl();//reduce T_P to increase throughput (demand > supply)
        }
        else {
            scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS)
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

                return awaitingReqCount > idleTaxiRegistry.getVehicleCount();

            default:
                throw new IllegalStateException();
        }
    }


    //request-initiated scheduling
    private void scheduleUnplannedRequestsImpl()
    {
        Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
        while (reqIter.hasNext() && idleTaxiRegistry.getVehicleCount() > 0) {
            TaxiRequest req = reqIter.next();

            Iterable<Vehicle> nearestVehs = idleTaxiRegistry
                    .findNearestVehicles(req.getFromLink().getFromNode(), 40);

            Iterable<Vehicle> filteredNearestVehs = vehicleFilter
                    .filterVehiclesForRequest(nearestVehs, req);
            
            BestDispatchFinder.Dispatch best = dispatchFinder.findBestVehicleForRequest(req,
                    filteredNearestVehs);

            //if (best != null) {
            optimConfig.scheduler.scheduleRequest(best.vehicle, best.request, best.path);
            
            reqIter.remove();
            unplannedRequestRegistry.removeRequest(req);
            idleTaxiRegistry.removeVehicle(best.vehicle);
            //}
        }
    }


    //vehicle-initiated scheduling
    private void scheduleIdleVehiclesImpl()
    {
        while (idleTaxiRegistry.getVehicleCount() > 0 && !unplannedRequests.isEmpty()) {
            Vehicle veh = idleTaxiRegistry.getVehicles().iterator().next();

            TaxiStayTask stayTask = (TaxiStayTask)veh.getSchedule().getCurrentTask();
            Iterable<TaxiRequest> nearestsReqs = unplannedRequestRegistry
                    .findNearestRequests(stayTask.getLink().getToNode(), 40);

            Iterable<TaxiRequest> filteredNearestReqs = requestFilter
                    .filterRequestsForVehicle(nearestsReqs, veh);

            BestDispatchFinder.Dispatch best = dispatchFinder.findBestRequestForVehicle(veh,
                    filteredNearestReqs);

            //if (best != null) {
            optimConfig.scheduler.scheduleRequest(best.vehicle, best.request, best.path);
            
            unplannedRequests.remove(best.request);
            unplannedRequestRegistry.removeRequest(best.request);
            idleTaxiRegistry.removeVehicle(veh);
            //}
        }
    }


    @Override
    public void requestSubmitted(Request request)
    {
        super.requestSubmitted(request);
        unplannedRequestRegistry.addRequest((TaxiRequest)request);
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);
        if (optimConfig.scheduler.isIdle(schedule.getVehicle())) {
            idleTaxiRegistry.addVehicle(schedule.getVehicle());
        }
    }


    @Override
    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return newCurrentTask.getTaxiTaskType() == TaxiTaskType.STAY;
    }
}
