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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;


public class KStraightLineNearestVehicleDepartureFilter
{
    private final int k;


    public KStraightLineNearestVehicleDepartureFilter(int k)
    {
        this.k = k;
    }


    public Iterable<VehicleData.Entry> filterVehiclesForRequest(
            Iterable<VehicleData.Entry> vehicles, TaxiRequest request)
    {
        Link toLink = request.getFromLink();
        PartialSort<VehicleData.Entry> nearestVehicleSort = new PartialSort<VehicleData.Entry>(k);

        for (VehicleData.Entry veh : vehicles) {
            double squaredDistance = DistanceUtils.calculateSquaredDistance(veh.link, toLink);
            nearestVehicleSort.add(veh, squaredDistance);
        }

        return nearestVehicleSort.retriveKSmallestElements();
    }
}
