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
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;


public class TaxiRequestCreator
    implements PassengerDepartureHandler.RequestCreator
{
    public static final String MODE = "taxi";

    private final VrpData vrpData;


    public TaxiRequestCreator(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    @Override
    public Request createRequest(MobsimAgent agent, MatsimVertex fromVertex, MatsimVertex toVertex,
            double now)
    {
        List<Customer> customers = vrpData.getCustomers();
        List<Request> requests = vrpData.getRequests();

        // agent -> customerId -> Customer
        int id = requests.size();
        Customer customer = new PassengerCustomer(id, fromVertex, agent);// TODO
        int duration = 120; // approx. 120 s for entering the taxi
        int t0 = (int)now;
        int t1 = t0 + 0; // hardcoded values!
        Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, 1, duration, t0,
                t1, false);
        customers.add(customer);
        requests.add(request);

        return request;
    }
}
