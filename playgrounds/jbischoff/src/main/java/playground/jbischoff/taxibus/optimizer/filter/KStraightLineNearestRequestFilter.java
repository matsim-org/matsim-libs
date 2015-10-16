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

package playground.jbischoff.taxibus.optimizer.filter;

import java.util.LinkedHashSet;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.util.DistanceUtils;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.passenger.TaxibusRequest.TaxibusRequestStatus;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPath;

import playground.michalm.util.PartialSort;


public class KStraightLineNearestRequestFilter
    implements TaxibusRequestFilter
{
    private final TaxibusScheduler scheduler;
    private final int k;


    public KStraightLineNearestRequestFilter(TaxibusScheduler scheduler, int k)
    {
        this.scheduler = scheduler;
        this.k = k;
    }


    @Override
    public List<TaxibusRequest> filterRequestsForVehicle(Iterable<TaxibusRequest> requests,
            Vehicle vehicle)
    {
        Link fromLink = scheduler.getImmediateDiversionOrEarliestIdleness(vehicle).link;
        PartialSort<TaxibusRequest> nearestRequestSort = new PartialSort<TaxibusRequest>(k);

        for (TaxibusRequest req : requests) {
            Link toLink = req.getFromLink();
            double squaredDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);
            nearestRequestSort.add(req, squaredDistance);
        }

        return nearestRequestSort.retriveKSmallestElements();
    }


	@Override
	public Iterable<TaxibusRequest> filterRequestsForBestRequest(Iterable<TaxibusRequest> unplannedRequests,
			TaxibusVehicleRequestPath best) {
		
		Link fromLink = best.path.get(0).getFromLink();
		Link initialToLink = best.getInitialDestination();
        PartialSort<TaxibusRequest> nearestRequestSort = new PartialSort<TaxibusRequest>(k);
        for (TaxibusRequest req : unplannedRequests) {
        	Link toLink = req.getFromLink();
            double squaredBeginDistance = DistanceUtils.calculateSquaredDistance(fromLink, toLink);
            
            Link reqDestinationLink = req.getToLink();
            double squaredDestinationDistance = DistanceUtils.calculateSquaredDistance(reqDestinationLink, initialToLink);
            nearestRequestSort.add(req, squaredBeginDistance+squaredDestinationDistance);
        }

        return nearestRequestSort.retriveKSmallestElements();

	}
}
