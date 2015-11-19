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

package playground.jbischoff.taxibus.optimizer.defaultOptimizer.filter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.util.DistanceUtils;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;
import playground.michalm.util.PartialSort;


public class KStraightLineNearestVehicleFilter
    implements TaxibusVehicleFilter
{
    private final TaxibusScheduler scheduler;
    private final int k;


    public KStraightLineNearestVehicleFilter(TaxibusScheduler scheduler, int k)
    {
        this.scheduler = scheduler;
        this.k = k;
    }


    @Override
    public Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles,
            TaxibusRequest request)
    {
        Link toLink = request.getFromLink();
        PartialSort<Vehicle> nearestVehicleSort = new PartialSort<Vehicle>(k);

        for (Vehicle veh : vehicles) {
        LinkTimePair ltp =   scheduler.getImmediateDiversionOrEarliestIdleness(veh);
        if (ltp!=null){
        	Link fromLink = ltp.link;
            double squaredDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);
            nearestVehicleSort.add(veh, squaredDistance);
        }
        }

        return nearestVehicleSort.retriveKSmallestElements();
    }
}
