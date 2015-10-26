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

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;


class AssignmentRequestData
{
    final List<TaxiRequest> requests = new ArrayList<>();
    final int urgentReqCount;
    final int dimension;


    AssignmentRequestData(TaxiOptimizerConfiguration optimConfig,
            SortedSet<TaxiRequest> unplannedRequests, double planningHorizon)
    {
        double currTime = optimConfig.context.getTime();
        double maxT0 = currTime + planningHorizon;
        int urgentReqCounter = 0;

        for (TaxiRequest r : unplannedRequests) {
            double t0 = r.getT0();
            if (t0 <= maxT0) {
                requests.add(r);
                if (t0 < currTime) {
                    urgentReqCounter++;
                }
            }
        }

        urgentReqCount = urgentReqCounter;
        dimension = unplannedRequests.size();
    }
}