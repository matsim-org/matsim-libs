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
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;


public class NOSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final VehicleFinder idleVehicleFinder;


    public NOSTaxiOptimizer(VrpData data, VrpPathCalculator calculator, Params params,
            boolean straightLineDistance)
    {
        this(data, calculator, params,
                new IdleVehicleFinder(data, calculator, straightLineDistance));
    }


    public NOSTaxiOptimizer(VrpData data, VrpPathCalculator calculator, Params params,
            VehicleFinder idleVehicleFinder)
    {
        super(data, calculator, params);
        this.idleVehicleFinder = idleVehicleFinder;
    }


    @Override
    protected VehiclePath findBestVehicle(TaxiRequest req, List<Vehicle> vehicles)
    {
        Vehicle veh = idleVehicleFinder.findVehicle(req);

        if (veh == null) {
            return VehiclePath.NO_VEHICLE_PATH_FOUND;
        }

        return super.findBestVehicle(req, Arrays.asList(veh));
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            return false;
        }

        if (unplannedRequestQueue.isEmpty()) {
            return false;
        }

        TaxiTask tt = schedule.getCurrentTask();
        switch (tt.getTaxiTaskType()) {
            case WAIT_STAY:
            case CRUISE_DRIVE:////////????????
                return true;

            default:
                return false;
        }
    }
}
