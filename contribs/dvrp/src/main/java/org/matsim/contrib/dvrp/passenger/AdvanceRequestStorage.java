/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.core.mobsim.framework.MobsimAgent;


public class AdvanceRequestStorage
{
    private final Map<Id<Person>, Queue<PassengerRequest>> advanceRequests = new HashMap<>();
    private final MatsimVrpContext context;


    AdvanceRequestStorage(MatsimVrpContext context)
    {
        this.context = context;
    }


    public void storeAdvanceRequest(PassengerRequest request)
    {
        Id<Person> passengerId = request.getPassenger().getId();
        Queue<PassengerRequest> passengerAdvReqs = advanceRequests.get(passengerId);

        if (passengerAdvReqs == null) {
            passengerAdvReqs = new PriorityQueue<>(3, Requests.T0_COMPARATOR);
            advanceRequests.put(passengerId, passengerAdvReqs);
        }

        passengerAdvReqs.add(request);
    }


    private boolean warningShown = false;


    public PassengerRequest retrieveAdvanceRequest(MobsimAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId)
    {
        Queue<PassengerRequest> passengerAdvReqs = advanceRequests.get(passenger.getId());

        if (passengerAdvReqs != null) {
            PassengerRequest req = passengerAdvReqs.peek();

            if (req != null) {
                if (req.getFromLink().getId() == fromLinkId //
                        && req.getToLink().getId() == toLinkId) {
                    // so this is the advance request for this leg
                    //
                    // This conclusion is based only on the from-to pair, we do not check how
                    // req.getT0() and context.getTime() relate to each other
                    //
                    // TODO should we verify if abs(T0-currTime) <= threshold 
                    passengerAdvReqs.poll();
                    return req;
                }
                else if (context.getTime() > req.getT0()) {
                    if (!warningShown) {
                        Logger.getLogger(getClass()).warn("There is an older prebooked request - "
                                + "seems that the agent has skipped a previously submitted request(??). "
                                + "This message is shown only once.");
                        warningShown = true;
                    }
                    //TODO michalm: do we have to somehow handle it (and how/where)?
                    //
                    //jb: my preference: ignore it. I had it turning up in multiple iterations and it doesnt do a thing (plans are sorted out because they end up being bad anyway)
                }
            }
        }

        return null;//this is an immediate request
    }
}
