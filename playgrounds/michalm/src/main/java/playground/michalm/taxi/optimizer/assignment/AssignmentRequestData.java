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

package playground.michalm.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Request;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerContext;


class AssignmentRequestData
{
    final List<TaxiRequest> requests = new ArrayList<>();
    final Map<Id<Request>, Integer> reqIdx = new HashMap<>();
    final int urgentReqCount;
    final int dimension;


    AssignmentRequestData(TaxiOptimizerContext optimContext,
            SortedSet<TaxiRequest> unplannedRequests, double planningHorizon)
    {
        double currTime = optimContext.context.getTime();
        double maxT0 = currTime + planningHorizon;
        int urgentReqCounter = 0;
        int idx = 0;

        for (TaxiRequest req : unplannedRequests) {
            double t0 = req.getT0();
            if (t0 <= maxT0) {
                requests.add(req);
                reqIdx.put(req.getId(), idx++);
                
                //'<=' or '<' does not make difference
                //(re-optimization is run before activity ends are handled)
                if (t0 <= currTime) {
                    urgentReqCounter++;
                }
            }
        }

        urgentReqCount = urgentReqCounter;
        dimension = unplannedRequests.size();
    }
}