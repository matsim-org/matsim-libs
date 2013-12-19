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

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.schedule.*;


public class IdleVehicleFinder
    implements VehicleFinder
{
    private final VrpData data;
    private final boolean straightLineDistance;


    public IdleVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
    }


    @Override
    public Vehicle findVehicle(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestDistance = Double.MAX_VALUE;

        for (Vehicle veh : data.getVehicles()) {
            double distance = calculateDistance(req, veh, data, straightLineDistance);

            if (distance < bestDistance) {
                bestVeh = veh;
                bestDistance = distance;
            }
        }

        return bestVeh;
    }


    public static double calculateDistance(TaxiRequest req, Vehicle veh, VrpData data,
            boolean straightLineDistance)
    {
        Schedule<TaxiTask> sched = TaxiSchedules.getSchedule(veh);
        int time = data.getTime();
        Vertex fromVertex;

        if (!TaxiUtils.isIdle(sched, time, true)) {
            return Double.MAX_VALUE;
        }

        TaxiTask currentTask = sched.getCurrentTask();

        switch (currentTask.getTaxiTaskType()) {
            case WAIT_STAY:
                fromVertex = ((StayTask)currentTask).getVertex();
                break;

            case CRUISE_DRIVE:
                throw new IllegalStateException();// currently, no support for vehicle diversion

            default:
                throw new IllegalStateException();
        }

        Vertex toVertex = req.getFromVertex();

        if (straightLineDistance) {
            double deltaX = toVertex.getX() - fromVertex.getX();
            double deltaY = toVertex.getY() - fromVertex.getY();

            // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() calls)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(time);
        }
    }
}
