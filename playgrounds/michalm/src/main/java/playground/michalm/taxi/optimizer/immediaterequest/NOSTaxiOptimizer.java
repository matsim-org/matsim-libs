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

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.data.schedule.*;
import org.matsim.contrib.dvrp.data.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;


public class NOSTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final VehicleFinder idleVehicleFinder;


    public NOSTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            int pickupDuration, int dropoffDuration, boolean straightLineDistance)
    {
        this(data, destinationKnown, minimizePickupTripTime, pickupDuration, dropoffDuration,
                new IdleVehicleFinder(data, straightLineDistance));
    }


    public NOSTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            int pickupDuration, int dropoffDuration, VehicleFinder idleVehicleFinder)
    {
        super(data, destinationKnown, minimizePickupTripTime, pickupDuration, dropoffDuration);
        this.idleVehicleFinder = idleVehicleFinder;
    }


    @Override
    protected VehicleDrive findBestVehicle(TaxiRequest req, List<Vehicle> vehicles)
    {
        Vehicle veh = idleVehicleFinder.findVehicle(req);

        if (veh == null) {
            return VehicleDrive.NO_VEHICLE_DRIVE_FOUND;
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
