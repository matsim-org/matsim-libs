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
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.model.MobsimAgentCustomer;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class TaxiModeDepartureHandler
    implements DepartureHandler
{
    public static final String TAXI_MODE = "taxi";

    private MatsimVrpData data;
    private TaxiSimEngine taxiSimEngine;


    public TaxiModeDepartureHandler(TaxiSimEngine taxiSimEngine, MatsimVrpData data)
    {
        this.taxiSimEngine = taxiSimEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id linkId)
    {
        if (agent.getMode().equals(TAXI_MODE)) {
            MatsimVrpGraph vrpGraph = data.getMatsimVrpGraph();
            MatsimVertex fromVertex = vrpGraph.getVertex(linkId);
            Id toLinkId = agent.getDestinationLinkId();
            MatsimVertex toVertex = vrpGraph.getVertex(toLinkId);

            List<Customer> customers = data.getVrpData().getCustomers();
            List<Request> requests = data.getVrpData().getRequests();

            // agent -> customerId -> Customer
            int id = requests.size();
            Customer customer = new MobsimAgentCustomer(id, fromVertex, agent);// TODO
            int duration = 120; // approx. 120 s for entering the taxi
            int t0 = (int)now;
            int t1 = t0 + 0; // hardcoded values!
            Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, 1, duration,
                    t0, t1, false);
            customers.add(customer);
            requests.add(request);

            taxiSimEngine.getInternalInterface().registerAdditionalAgentOnLink(agent);
            taxiSimEngine.taxiRequestSubmitted(request, now);

            return true;
        }
        else {
            return false;
        }
    }
}
