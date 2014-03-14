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
import playground.michalm.taxi.optimizer.*;

import com.google.common.collect.Iterables;


class MIPRequestData
{
    static final int REQS_PER_VEH = 10;

    final TaxiRequest[] requests;
    final Map<Id, Integer> reqIdToIdx = new HashMap<Id, Integer>();
    final int dimension;


    MIPRequestData(TaxiOptimizerConfiguration optimConfig,
            SortedSet<TaxiRequest> unplannedRequests, VehicleData vData)
    {
        int reqLimit = REQS_PER_VEH * vData.dimension;
        dimension = Math.min(reqLimit, unplannedRequests.size());

        requests = Iterables.toArray(Iterables.limit(unplannedRequests, dimension),
                TaxiRequest.class);

        for (int i = 0; i < dimension; i++) {
            reqIdToIdx.put(requests[i].getId(), i);
        }
    }
}
