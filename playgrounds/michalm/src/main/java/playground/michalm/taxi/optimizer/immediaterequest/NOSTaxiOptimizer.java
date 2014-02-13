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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;


public class NOSTaxiOptimizer
    implements ImmediateRequestTaxiOptimizer
{
    private final TaxiScheduler scheduler;

    private final VehicleFinder idleVehicleFinder;
    private final boolean seekDemandSupplyEquilibrium;

    private final Queue<TaxiRequest> unplannedRequests;
    private final Queue<Vehicle> idleVehicles;

    private boolean requiresReoptimization = false;


    public NOSTaxiOptimizer(TaxiScheduler scheduler, MatsimVrpContext context,
            VehicleFinder idleVehicleFinder, boolean seekDemandSupplyEquilibrium)
    {
        this.scheduler = scheduler;

        this.idleVehicleFinder = idleVehicleFinder;
        this.seekDemandSupplyEquilibrium = seekDemandSupplyEquilibrium;

        int vehCount = context.getVrpData().getVehicles().size();

        unplannedRequests = new ArrayDeque<TaxiRequest>(vehCount);//1 req per veh
        idleVehicles = new ArrayDeque<Vehicle>(vehCount);
    }


    //==============================

    /**
     * Try to schedule all unplanned tasks (if any)
     */
    protected void scheduleUnplannedRequests()
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            Vehicle veh = idleVehicleFinder.findVehicle(idleVehicles, req);

            if (veh == null) {
                return;
            }

            VehicleRequestPath best = new VehicleRequestPath(veh, req, scheduler.calculateVrpPath(
                    veh, req));

            scheduler.scheduleRequest(best);
            unplannedRequests.poll();
            idleVehicles.remove(veh);
        }
    }


    //==============================

    private void scheduleIdleVehicles()
    {
        while (!idleVehicles.isEmpty()) {
            Vehicle veh = idleVehicles.peek();

            VehicleRequestPath best = scheduler.findBestVehicleRequestPath(veh, unplannedRequests);

            if (best == null) {
                return;//no unplanned requests
            }

            scheduler.scheduleRequest(best);
            unplannedRequests.remove(best.request);
            idleVehicles.remove(veh);
        }
    }


    //==============================

    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (!requiresReoptimization) {
            return;
        }

        if (seekDemandSupplyEquilibrium) {
            if (unplannedRequests.size() > idleVehicles.size()) {
                scheduleIdleVehicles();//reduce T_P to increase throughput (demand > supply)
            }
            else {
                scheduleUnplannedRequests();//reduce T_W (otherwise)
            }
        }
        else {
            if (scheduler.getParams().minimizePickupTripTime) {
                scheduleIdleVehicles();//reduce T_P
            }
            else {
                scheduleUnplannedRequests();//reduce T_W (regular NOS)
            }
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

        scheduler.updateBeforeNextTask(taxiSchedule);
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


    @Override
    public TaxiScheduler getScheduler()
    {
        return scheduler;
    }
}
