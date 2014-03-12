/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.mip;

import java.util.*;

import org.matsim.api.core.v01.Id;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;


/*package*/class MIPRequestData
{
    public static final int REQS_PER_VEH = 5;

    /*package*/final TaxiRequest[] requests;
    /*package*/Map<Id, Integer> reqIdToIdx = new HashMap<Id, Integer>();
    /*package*/final int dimension;


    /*package*/MIPRequestData(TaxiOptimizerConfiguration optimConfig,
            SortedSet<TaxiRequest> unplannedRequests)
    {
        int reqLimit = REQS_PER_VEH * optimConfig.context.getVrpData().getVehicles().size();
        dimension = Math.min(reqLimit, unplannedRequests.size());

        Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
        requests = new TaxiRequest[dimension];

        for (int i = 0; i < dimension; i++) {
            TaxiRequest req = reqIter.next();
            requests[i] = req;
            reqIdToIdx.put(req.getId(), i);
        }
    }
}
