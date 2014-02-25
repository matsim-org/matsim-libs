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

package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.*;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.vehreqpath.*;


public class NOSTaxiOptimizer
    implements ImmediateRequestTaxiOptimizer
{
    private final OptimizerConfiguration optimConfig;

    private final VehicleFinder idleVehicleFinder;
    private final boolean seekDemandSupplyEquilibrium;

    private final Queue<TaxiRequest> unplannedRequests;
    private final Queue<Vehicle> idleVehicles;

    private boolean requiresReoptimization = false;


    public NOSTaxiOptimizer(OptimizerConfiguration optimConfig, VehicleFinder idleVehicleFinder,
            boolean seekDemandSupplyEquilibrium)
    {
        this.optimConfig = optimConfig;

        this.idleVehicleFinder = idleVehicleFinder;
        this.seekDemandSupplyEquilibrium = seekDemandSupplyEquilibrium;

        int vehCount = optimConfig.context.getVrpData().getVehicles().size();

        unplannedRequests = new PriorityQueue<TaxiRequest>(vehCount,//1 req per 1 veh
                Requests.T0_COMPARATOR);
        idleVehicles = new ArrayDeque<Vehicle>(vehCount);
    }


    @Override
    public OptimizerConfiguration getOptimizerConfiguration()
    {
        return optimConfig;
    }


    //==============================

    /**
     * Try to schedule all unplanned tasks (if any)
     */
    protected void scheduleUnplannedRequests()
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            Vehicle veh = idleVehicleFinder.findBestVehicleForRequest(idleVehicles, req);

            if (veh == null) {
                return;
            }

            VehicleRequestPath best = new VehicleRequestPath(veh, req,
                    optimConfig.vrpFinder.calculateVrpPath(veh, req));

            optimConfig.scheduler.scheduleRequest(best);
            unplannedRequests.poll();
            idleVehicles.remove(veh);
        }
    }


    //==============================

    //TODO straight-line distance not supported
    private void scheduleIdleVehicles()
    {
        while (!idleVehicles.isEmpty()) {
            Vehicle veh = idleVehicles.peek();

            VehicleRequestPath best = optimConfig.vrpFinder.findBestRequestForVehicle(veh,
                    unplannedRequests, VehicleRequestPaths.TP_COMPARATOR);

            if (best == null) {
                return;//no unplanned requests
            }

            optimConfig.scheduler.scheduleRequest(best);
            unplannedRequests.remove(best.request);
            idleVehicles.remove(veh);
        }
    }


    private boolean doReduceTP()
    {
        if (!seekDemandSupplyEquilibrium) {
            return optimConfig.params.minimizePickupTripTime;
        }
        else {
            int awaitingReqCount = DemandSupplyEquilibriumUtils.countAwaitingUnplannedRequests(
                    unplannedRequests, optimConfig.context.getTime());
            return awaitingReqCount > idleVehicles.size();
        }
    }


    //==============================

    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (!requiresReoptimization) {
            return;
        }

        if (doReduceTP()) {
            scheduleIdleVehicles();//reduce T_P to increase throughput (demand > supply)
        }
        else {
            scheduleUnplannedRequests();//reduce T_W (regular NOS)
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
        taxiSchedule.nextTask();

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            idleVehicles.remove(schedule.getVehicle());
            return;
        }

        switch (taxiSchedule.getCurrentTask().getTaxiTaskType()) {
            case WAIT_STAY:
                idleVehicles.add(schedule.getVehicle());
                requiresReoptimization = true;
                break;

            case PICKUP_DRIVE:
                idleVehicles.remove(schedule.getVehicle());
                break;

            default:
                //do nothing
        }
    };


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {}
}
