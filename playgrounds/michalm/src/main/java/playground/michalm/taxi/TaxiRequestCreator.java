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

package playground.michalm.taxi;

import java.util.List;

import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.taxi.model.TaxiRequest;


public class TaxiRequestCreator
    implements PassengerRequestCreator
{
    public static final String MODE = "taxi";

    private final VrpData vrpData;


    public TaxiRequestCreator(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    @Override
    public TaxiRequest createRequest(Customer customer, MatsimVertex fromVertex,
            MatsimVertex toVertex, double now)
    {
        List<Request> requests = vrpData.getRequests();

        int id = requests.size();
        TaxiRequest request = new TaxiRequest(id, customer, fromVertex, toVertex, (int)now,
                (int)now);
        requests.add(request);

        return request;
    }
}
