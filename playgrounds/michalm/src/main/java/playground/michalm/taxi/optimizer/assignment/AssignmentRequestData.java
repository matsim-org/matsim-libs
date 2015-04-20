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

import java.util.SortedSet;

import org.matsim.contrib.dvrp.data.Requests;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;


class AssignmentRequestData
{
    final TaxiRequest[] requests;
    final int urgentReqCount;
    final int dimension;


    AssignmentRequestData(TaxiOptimizerConfiguration optimConfig, SortedSet<TaxiRequest> unplannedRequests)
    {
        dimension = unplannedRequests.size();//TODO - consider only awaiting and "soon-awaiting" reqs!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        urgentReqCount = Requests.countRequests(unplannedRequests, new Requests.IsUrgentPredicate(
                optimConfig.context.getTime()));

        requests = unplannedRequests.toArray(new TaxiRequest[dimension]);
    }
}