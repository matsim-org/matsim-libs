/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.algorithm.passenger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;


/**
 * @author jbischoff
 */
public class TaxibusPassengerEngine
    extends PassengerEngine
{

    private int abortWarn = 0;


    public TaxibusPassengerEngine(String mode, EventsManager eventsManager,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer, VrpData vrpData,
            Network network)
    {
        super(mode, eventsManager, requestCreator, optimizer, vrpData, network);
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId)
    {
        if (!agent.getMode().equals(getMode())) {
            return false;
        }

        boolean rejected = (!super.handleDeparture(now, agent, fromLinkId));

        //FIXME
        //there is no dismissal of immediate requests; on the other hand, the prior solution was
        //not perfect(??)
        //
        //FIX:
        //setRejected(true) in RequestCreator if immediate request;
        //optimizer should ignore rejected requests
        
        if (rejected) {
            if (abortWarn < 10)
                Logger.getLogger(getClass())
                        .error(agent.getId().toString() + " is aborted, no Taxibus was found");
            abortWarn++;
            if (abortWarn == 10)
                Logger.getLogger(getClass())
                        .error("no more aborted taxibus agents will be displayed");
            //		        	agent.setStateToAbort(now);
        }
        	
        
        return (!rejected);
    }


    public boolean prebookTrip(double now, MobsimPassengerAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId, double departureTime)
    {
        
        if (departureTime <= now) {
            Logger.getLogger(this.getClass()).info(
                    "This is not a call ahead (departure time: " + departureTime + " now: " + now);
            departureTime = now + 1;
        }

        return super.prebookTrip(now, passenger, fromLinkId, toLinkId, departureTime);
    }
}
