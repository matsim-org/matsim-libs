/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.tsp;

import java.util.List;

import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.dvrp.passenger.RequestCreator;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;


public class TSPRequestCreator
    implements RequestCreator
{
    public static final String MODE = "call_salesman";

    private final VrpData vrpData;


    public TSPRequestCreator(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    @Override
    public Request createRequest(Customer customer, MatsimVertex fromVertex, MatsimVertex toVertex,
            double startTime)
    {
        List<Request> requests = vrpData.getRequests();

        int id = requests.size();
        int t0 = (int)startTime;
        int t1 = t0; // no time window
        Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, t0, t1,
                vrpData.getTime());

        requests.add(request);
        return request;
    }
}
