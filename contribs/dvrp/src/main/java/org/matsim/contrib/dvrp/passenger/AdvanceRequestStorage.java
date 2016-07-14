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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.core.mobsim.framework.MobsimAgent;


class AdvanceRequestStorage
{
    private final Map<Id<Person>, Queue<PassengerRequest>> advanceRequests = new HashMap<>();


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


    public PassengerRequest retrieveAdvanceRequest(MobsimAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId, double now)
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
                else if (now > req.getT0()) {
                    //TODO do we have to somehow handle it?
                    //Currently this is not a problem; we do not have cases of not turning up...
                    throw new IllegalStateException(
                            "Seems that the agent has skipped a previously submitted request");
                }
            }
        }

        return null;//this is an immediate request
    }
}
