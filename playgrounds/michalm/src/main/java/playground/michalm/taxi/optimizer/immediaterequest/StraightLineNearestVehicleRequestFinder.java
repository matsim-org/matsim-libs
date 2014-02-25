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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.util.*;

import playground.michalm.taxi.model.TaxiRequest;


public class StraightLineNearestVehicleRequestFinder
    implements VehicleFinder
{
    private final TaxiScheduler scheduler;


    public StraightLineNearestVehicleRequestFinder(TaxiScheduler scheduler)
    {
        this.scheduler = scheduler;
    }


    @Override
    public Vehicle findBestVehicleForRequest(Iterable<Vehicle> vehicles, TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestCost = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {
            LinkTimePair departure = scheduler.getEarliestIdleness(veh);
            Link fromLink = departure.link;
            Link toLink = req.getFromLink();

            double cost = DistanceUtils.calculateSquareDistance(fromLink, toLink);

            if (cost < bestCost) {
                bestVeh = veh;
                bestCost = cost;
            }
        }

        return bestVeh;
    }
    
    
    public Vehicle findBestRequestForVehicle(Iterable<Vehicle> vehicles, TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestCost = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {
            LinkTimePair departure = scheduler.getEarliestIdleness(veh);
            Link fromLink = departure.link;
            Link toLink = req.getFromLink();

            double cost = DistanceUtils.calculateSquareDistance(fromLink, toLink);

            if (cost < bestCost) {
                bestVeh = veh;
                bestCost = cost;
            }
        }

        return bestVeh;
    }
}
