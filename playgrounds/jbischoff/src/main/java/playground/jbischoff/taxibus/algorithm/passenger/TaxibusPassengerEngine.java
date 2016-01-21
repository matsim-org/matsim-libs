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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author  jbischoff
 *
 */
public class TaxibusPassengerEngine extends PassengerEngine {

	private int abortWarn = 0;
	public TaxibusPassengerEngine(String mode, EventsManager eventsManager, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, MatsimVrpContext context) {
		super(mode, eventsManager, requestCreator, optimizer, context);
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		   {
		        if (!agent.getMode().equals(mode)) {
		            return false;
		        }

		        MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;

		        Id<Link> toLinkId = passenger.getDestinationLinkId();


		        internalInterface.registerAdditionalAgentOnLink(passenger);
		        PassengerRequest request = advanceRequestStorage.retrieveAdvanceRequest(passenger,
		                fromLinkId, toLinkId);

		        if (request == null) {
		        	return false;
		        	//we don't want immediate requests for Taxibus
		        	
		        	//shouldn't we throw here some exception? michal, nov'15
		        }
		        else {
		            PassengerPickupActivity awaitingPickup = awaitingPickupStorage
		                    .retrieveAwaitingPickup(request);

		            if (awaitingPickup != null) {
		                awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
		            }
		        }
		        if (request.isRejected()){
		        	
		        	if (abortWarn<10) Logger.getLogger(getClass()).error(agent.getId().toString() + " is aborted, no Taxibus was found");
		        	abortWarn++;
		        	if (abortWarn==10) Logger.getLogger(getClass()).error("no more aborted taxibus agents will be displayed");
//		        	agent.setStateToAbort(now);
		        }
		        return !request.isRejected();
		    }
		
		
	}

	public boolean prebookTrip(double now, MobsimPassengerAgent passenger, Id<Link> fromLinkId,
			Id<Link> toLinkId, Double departureTime) {
		    		        if (departureTime <= now ) {
		            Logger.getLogger(this.getClass()).info("This is not a call ahead (departure time: "+departureTime+" now: "+now);
		            departureTime = now+1;
		        }

		        PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime+1,
		                now);
                optimizer.requestSubmitted(request);

                if (!request.isRejected()) {
                    advanceRequestStorage.storeAdvanceRequest(request);
                }

		        return !request.isRejected();
		    
		
	}
}
