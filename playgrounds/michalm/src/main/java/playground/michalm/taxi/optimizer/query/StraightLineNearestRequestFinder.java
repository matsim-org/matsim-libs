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

import java.util.Collections;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.DistanceUtils;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.fifo.TaxiScheduler;


public class StraightLineNearestRequestFinder
    implements RequestFinder, RequestFilter
{
    private final TaxiScheduler scheduler;


    public StraightLineNearestRequestFinder(TaxiScheduler scheduler)
    {
        this.scheduler = scheduler;
    }


    @Override
    public TaxiRequest findRequestForVehicle(Iterable<TaxiRequest> requests, Vehicle vehicle)
    {
        Link fromLink = scheduler.getEarliestIdleness(vehicle).link;
        TaxiRequest bestReq = null;
        double bestSquaredDistance = Double.MAX_VALUE;

        for (TaxiRequest req : requests) {
            Link toLink = req.getFromLink();

            double squaredDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);

            if (squaredDistance < bestSquaredDistance) {
                bestReq = req;
                bestSquaredDistance = squaredDistance;
            }
        }

        return bestReq;
    }


    @Override
    public Iterable<TaxiRequest> filterRequestsForVehicle(Iterable<TaxiRequest> requests,
            Vehicle vehicle)
    {
        return Collections.singletonList(findRequestForVehicle(requests, vehicle));
    }
}
