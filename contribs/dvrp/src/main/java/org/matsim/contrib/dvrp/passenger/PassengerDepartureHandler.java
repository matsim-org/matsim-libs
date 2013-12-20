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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;


public class PassengerDepartureHandler
    implements DepartureHandler
{
    private final String mode;
    private final PassengerRequestCreator requestCreator;
    private final MatsimVrpData data;
    private final VrpSimEngine vrpSimEngine;


    public PassengerDepartureHandler(String mode, PassengerRequestCreator requestCreator,
            VrpSimEngine vrpSimEngine, MatsimVrpData data)
    {
        this.mode = mode;
        this.requestCreator = requestCreator;
        this.vrpSimEngine = vrpSimEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent passengerAgent, Id fromLinkId)
    {
        if (!passengerAgent.getMode().equals(mode)) {
            return false;
        }

        vrpSimEngine.getInternalInterface().registerAdditionalAgentOnLink(passengerAgent);
        Id toLinkId = passengerAgent.getDestinationLinkId();

        MatsimVrpGraph vrpGraph = data.getMatsimVrpGraph();
        MatsimVertex fromVertex = vrpGraph.getVertex(fromLinkId);
        MatsimVertex toVertex = vrpGraph.getVertex(toLinkId);

        PassengerCustomer customer = PassengerCustomer
                .getOrCreatePassengerCustomer(data, passengerAgent);
        List<PassengerRequest> submittedReqs = customer.getRequests();

        for (PassengerRequest r : submittedReqs) {
            if (r.getFromVertex() == fromVertex && r.getToVertex() == toVertex && r.getT0() <= now
                    && r.getT1() + 1 >= now) {
                //This is it! This is an advance request, so do not resubmit a duplicate!
                return true;
            }
        }

        PassengerRequest request = requestCreator
                .createRequest(customer, fromVertex, toVertex, now);
        submittedReqs.add(request);
        vrpSimEngine.requestSubmitted(request, now);

        return true;
    }
}
