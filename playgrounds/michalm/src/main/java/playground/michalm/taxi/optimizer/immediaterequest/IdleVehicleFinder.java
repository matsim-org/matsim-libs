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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.schedule.*;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.schedule.*;


public class IdleVehicleFinder
    implements VehicleFinder
{
    private final MatsimVrpContext context;
    private final VrpPathCalculator calculator;
    private final boolean straightLineDistance;


    public IdleVehicleFinder(MatsimVrpContext context, VrpPathCalculator calculator,
            boolean straightLineDistance)
    {
        this.context = context;
        this.calculator = calculator;
        this.straightLineDistance = straightLineDistance;
    }


    @Override
    public Vehicle findVehicle(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestDistance = Double.MAX_VALUE;

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            double distance = calculateDistance(req, veh, context.getTime(), calculator,
                    straightLineDistance);

            if (distance < bestDistance) {
                bestVeh = veh;
                bestDistance = distance;
            }
        }

        return bestVeh;
    }


    public static double calculateDistance(TaxiRequest req, Vehicle veh, double time,
            VrpPathCalculator calculator, boolean straightLineDistance)
    {
        Schedule<TaxiTask> sched = TaxiSchedules.getSchedule(veh);
        Link fromLink;

        if (!TaxiUtils.isIdle(sched, time, true)) {
            return Double.MAX_VALUE;
        }

        TaxiTask currentTask = sched.getCurrentTask();

        switch (currentTask.getTaxiTaskType()) {
            case WAIT_STAY:
                fromLink = ((StayTask)currentTask).getLink();
                break;

            case CRUISE_DRIVE:
                throw new IllegalStateException();// currently, no support for vehicle diversion

            default:
                throw new IllegalStateException();
        }

        Link toLink = req.getFromLink();

        if (straightLineDistance) {
            Coord fromCoord = fromLink.getCoord();
            Coord toCoord = toLink.getCoord();

            double deltaX = toCoord.getX() - fromCoord.getX();
            double deltaY = toCoord.getY() - fromCoord.getY();

            // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() calls)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
            //TODO consider storing the shortest path (sth like BestShortestPath object)
            return calculator.calcPath(fromLink, toLink, time).getTravelCost();
        }
    }
}
