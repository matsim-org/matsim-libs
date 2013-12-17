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

package org.matsim.contrib.dvrp.examples.dapp;

import java.util.List;

import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.dvrp.passenger.RequestCreator;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.DeliveryRequest;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.impl.DeliveryRequestImpl;


public class DAPPRequestCreator
    implements RequestCreator
{
    public static final String MODE = "dial_a_pizza";

    private final VrpData vrpData;


    public DAPPRequestCreator(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    @Override
    public DeliveryRequest createRequest(Customer customer, MatsimVertex fromVertex,
            MatsimVertex toVertex, double startTime)
    {
        List<Request> requests = vrpData.getRequests();

        int id = requests.size();
        int t0 = (int)startTime;
        int t1 = t0; // no time window
        DeliveryRequest request = new DeliveryRequestImpl(id, customer, 1, t0, t1,
                vrpData.getTime(), toVertex);

        requests.add(request);
        return request;
    }
}
