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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.model.*;
import org.matsim.contrib.dvrp.passenger.*;


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
    public PassengerRequest createRequest(Customer customer, Link fromLink, Link toLink, double now)
    {
        List<Request> requests = vrpData.getRequests();
        OneTaxiRequest request = new OneTaxiRequest(customer, fromLink, toLink, now);
        requests.add(request);
        return request;
    }
}
