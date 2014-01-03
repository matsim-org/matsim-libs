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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.taxi.model.TaxiRequest;


public class TaxiRequestCreator
    implements PassengerRequestCreator
{
    public static final String MODE = "taxi";

    private final MatsimVrpData data;


    public TaxiRequestCreator(MatsimVrpData data)
    {
        this.data = data;
    }


    @Override
    public TaxiRequest createRequest(Customer customer, Link fromLink, Link toLink, double now)
    {
        List<Request> requests = data.getVrpData().getRequests();

        Id id = data.getScenario().createId(requests.size() + "");
        TaxiRequest request = new TaxiRequest(id, customer, fromLink, toLink, (int)now, (int)now);
        requests.add(request);

        return request;
    }
}
