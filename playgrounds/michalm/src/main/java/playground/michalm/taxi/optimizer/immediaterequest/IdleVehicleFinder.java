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
{
    private final VrpData data;
    private final boolean straightLineDistance;


    public IdleVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
    }


    public Vehicle findClosestVehicle(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestDistance = Double.MAX_VALUE;

        for (Vehicle veh : data.getVehicles()) {
            double distance = calculateDistance(req, veh);

            if (distance < bestDistance) {
                bestVeh = veh;
                bestDistance = distance;
            }
        }

        return bestVeh;
    }


    private double calculateDistance(TaxiRequest req, Vehicle veh)
    {
        Schedule<TaxiTask> sched = TaxiSchedules.getSchedule(veh);
        int time = data.getTime();
        Vertex departVertex;

        if (!TaxiUtils.isIdle(sched, time, true)) {
            return Double.MAX_VALUE;
        }

        TaxiTask currentTask = sched.getCurrentTask();

        switch (currentTask.getTaxiTaskType()) {
            case WAIT_STAY:
                departVertex = ((StayTask)currentTask).getVertex();
                break;

            case CRUISE_DRIVE:
                throw new IllegalStateException();// currently, no support for vehicle diversion

            default:
                throw new IllegalStateException();
        }

        return distance(departVertex, req.getFromVertex(), time);
    }


    private double distance(Vertex fromVertex, Vertex toVertex, int departTime)
    {
        if (straightLineDistance) {
            double deltaX = toVertex.getX() - fromVertex.getX();
            double deltaY = toVertex.getY() - fromVertex.getY();

            // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() call)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(departTime);
        }
    }
}
