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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;


public class NOSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final VehicleFinder idleVehicleFinder;
    private final boolean considerAllRequestForIdleVehicle;


    public NOSTaxiOptimizer(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params, VehicleFinder idleVehicleFinder,
            boolean considerAllRequestForIdleVehicle)
    {
        super(context, calculator, params);
        this.idleVehicleFinder = idleVehicleFinder;
        this.considerAllRequestForIdleVehicle = considerAllRequestForIdleVehicle;
    }


    @Override
    protected VehicleRequestPath findBestVehicleRequestPath(TaxiRequest req)
    {
        Vehicle veh = idleVehicleFinder.findVehicle(req);
        return veh == null ? null : new VehicleRequestPath(veh, req, calculateVrpPath(veh, req));
    }


    private void scheduleIdleVehicle(Vehicle veh)
    {
        VehicleRequestPath best = findBestVehicleRequestPath(veh);

        if (best == null) {
            return;//no unplanned requests
        }

        scheduleRequestImpl(best);
        unplannedRequests.remove(best.request);
    }


    protected VehicleRequestPath findBestVehicleRequestPath(Vehicle veh)
    {
        VehicleRequestPath best = null;

        for (TaxiRequest req : unplannedRequests) {
            VrpPathWithTravelData current = calculateVrpPath(veh, req);

            if (current == null) {
                continue;
            }
            else if (best == null) {
                best = new VehicleRequestPath(veh, req, current);
            }
            else if (pathComparator.compare(current, best.path) < 0) {
                // TODO: in the future: add a check if the taxi time windows are satisfied
                best = new VehicleRequestPath(veh, req, current);
            }
        }

        return best;
    }


    @Override
    protected void nextTask(Schedule<TaxiTask> schedule, boolean scheduleUpdated)
    {
        schedule.nextTask();

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            return;
        }

        if (unplannedRequests.isEmpty()) {
            return;
        }

        TaxiTask tt = schedule.getCurrentTask();
        switch (tt.getTaxiTaskType()) {
            case WAIT_STAY:
            case CRUISE_DRIVE:////////????????
                if (considerAllRequestForIdleVehicle) {
                    scheduleIdleVehicle(schedule.getVehicle());//schedules the BEST request
                }
                else {
                    scheduleUnplannedRequests();//schedules the FIRST request
                }
                return;

            default:
                return;
        }
    }
}
