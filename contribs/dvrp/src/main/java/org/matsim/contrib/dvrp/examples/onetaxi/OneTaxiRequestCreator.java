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

package org.matsim.contrib.dvrp.examples.onetaxi;

import java.util.List;

import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.dvrp.passenger.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;


public class OneTaxiRequestCreator
    implements PassengerRequestCreator
{
    public static final String MODE = "taxi";

    private final VrpData vrpData;


    public OneTaxiRequestCreator(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    @Override
    public PassengerRequest createRequest(Customer customer, MatsimVertex fromVertex,
            MatsimVertex toVertex, double now)
    {
        List<Request> requests = vrpData.getRequests();

        int id = requests.size();
        OneTaxiRequest request = new OneTaxiRequest(id, customer, fromVertex, toVertex, (int)now);

        requests.add(request);
        return request;
    }
}
