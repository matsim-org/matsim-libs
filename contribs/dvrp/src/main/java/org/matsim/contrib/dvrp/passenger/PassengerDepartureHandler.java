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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import pl.poznan.put.vrp.dynamic.data.model.Request;


public class PassengerDepartureHandler
    implements DepartureHandler
{
    private final String mode;
    private final RequestCreator requestCreator;
    private final MatsimVrpData data;
    private final VrpSimEngine vrpSimEngine;


    public PassengerDepartureHandler(String mode, RequestCreator requestCreator,
            VrpSimEngine vrpSimEngine, MatsimVrpData data)
    {
        this.mode = mode;
        this.requestCreator = requestCreator;
        this.vrpSimEngine = vrpSimEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id linkId)
    {
        if (agent.getMode().equals(mode)) {
            vrpSimEngine.getInternalInterface().registerAdditionalAgentOnLink(agent);
            
            if (agent.getId().toString().equals("0031495")) {
                System.out.println("aaa");
            }

            MatsimVrpGraph vrpGraph = data.getMatsimVrpGraph();
            MatsimVertex fromVertex = vrpGraph.getVertex(linkId);
            Id toLinkId = agent.getDestinationLinkId();
            MatsimVertex toVertex = vrpGraph.getVertex(toLinkId);

            boolean submitted = false;

            //TODO this "submitted?" check works only for up to 1 advanced request per customer
            for (Request req : data.getVrpData().getRequests()) {
                if ( ((PassengerCustomer)req.getCustomer()).getPassenger() == agent) {
                    submitted = true;
                }
            }

            if (!submitted) {
                PassengerCustomer customer = PassengerCustomer.getOrCreatePassengerCustomer(data,
                        agent);
                Request request = requestCreator.createRequest(customer, fromVertex, toVertex, now);
                vrpSimEngine.requestSubmitted(request, now);
            }

            return true;
        }
        else {
            return false;
        }
    }
}
