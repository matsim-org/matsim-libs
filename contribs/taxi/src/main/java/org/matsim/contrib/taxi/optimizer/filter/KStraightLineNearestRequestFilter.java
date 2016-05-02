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

package org.matsim.contrib.taxi.optimizer.filter;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;


public class KStraightLineNearestRequestFilter
{
    private final TaxiScheduleInquiry scheduleInquiry;
    private final int k;


    public KStraightLineNearestRequestFilter(TaxiScheduleInquiry scheduleInquiry, int k)
    {
        this.scheduleInquiry = scheduleInquiry;
        this.k = k;
    }


    public List<TaxiRequest> filterRequestsForVehicle(Iterable<TaxiRequest> requests,
            Vehicle vehicle)
    {
        Link fromLink = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(vehicle).link;
        PartialSort<TaxiRequest> nearestRequestSort = new PartialSort<TaxiRequest>(k);

        for (TaxiRequest req : requests) {
            Link toLink = req.getFromLink();
            double squaredDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);
            nearestRequestSort.add(req, squaredDistance);
        }

        return nearestRequestSort.retriveKSmallestElements();
    }
}
