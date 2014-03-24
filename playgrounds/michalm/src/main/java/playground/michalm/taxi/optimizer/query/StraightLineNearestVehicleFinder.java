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

package playground.michalm.taxi.optimizer.query;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.DistanceUtils;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.scheduler.TaxiScheduler;


public class StraightLineNearestVehicleFinder
    implements VehicleFinder, VehicleFilter
{
    private final TaxiScheduler scheduler;


    public StraightLineNearestVehicleFinder(TaxiScheduler scheduler)
    {
        this.scheduler = scheduler;
    }


    @Override
    public Vehicle findVehicleForRequest(Iterable<Vehicle> vehicles, TaxiRequest req)
    {
        Link toLink = req.getFromLink();
        Vehicle bestVeh = null;
        double bestSquaredDistance = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {
            Link fromLink = scheduler.getEarliestIdleness(veh).link;

            double squaredDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);

            if (squaredDistance < bestSquaredDistance) {
                bestVeh = veh;
                bestSquaredDistance = squaredDistance;
            }
        }

        return bestVeh;
    }


    @Override
    public Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles,
            TaxiRequest request)
    {
        return QueryUtils.toIterableExcludingNull(findVehicleForRequest(vehicles, request));
    }
}
