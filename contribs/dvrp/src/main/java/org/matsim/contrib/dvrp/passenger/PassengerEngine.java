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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;


public class PassengerEngine
    implements MobsimEngine, DepartureHandler
{
    private final String mode;

    private InternalInterface internalInterface;
    private final MatsimVrpContext context;
    private final PassengerRequestCreator requestCreator;
    private final VrpOptimizer optimizer;


    public PassengerEngine(String mode, PassengerRequestCreator requestCreator,
            VrpOptimizer optimizer, MatsimVrpContext context)
    {
        this.mode = mode;
        this.requestCreator = requestCreator;
        this.optimizer = optimizer;
        this.context = context;
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


    public String getMode()
    {
        return mode;
    }


    @Override
    public void onPrepareSim()
    {}


    @Override
    public void doSimStep(double time)
    {}


    @Override
    public void afterSim()
    {}


    public boolean callAhead(double now, MobsimPassengerAgent passenger, Leg leg)
    {
        if (!leg.getMode().equals(mode)) {
            return false;
        }

        if (leg.getDepartureTime() <= now) {
            throw new IllegalStateException("This is not a call ahead");
        }

        Id fromLinkId = leg.getRoute().getStartLinkId();
        Id toLinkId = leg.getRoute().getEndLinkId();
        double departureTime = leg.getDepartureTime();

        PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime,
                now);
        storeAdvancedRequest(request);

        optimizer.requestSubmitted(request);
        return true;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id fromLinkId)
    {
        if (!agent.getMode().equals(mode)) {
            return false;
        }

        MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;

        Id toLinkId = passenger.getDestinationLinkId();
        double departureTime = now;

        internalInterface.registerAdditionalAgentOnLink(passenger);

        PassengerRequest request = retrieveAdvancedRequest(passenger, fromLinkId, toLinkId);

        if (request == null) {//this is an immediate request
            request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
            optimizer.requestSubmitted(request);
        }
        else {
            PassengerPickupActivity awaitingPickup = retrieveAwaitingPickup(request);

            if (awaitingPickup != null) {
                awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
            }
        }

        return true;
    }


    //================ REQUESTS CREATION

    private PassengerRequest createRequest(MobsimPassengerAgent passenger, Id fromLinkId,
            Id toLinkId, double departureTime, double now)
    {
        Map<Id, ? extends Link> links = context.getScenario().getNetwork().getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);

        List<Request> requests = context.getVrpData().getRequests();
        Id id = context.getScenario().createId(requests.size() + "");

        PassengerRequest request = requestCreator.createRequest(id, passenger, fromLink, toLink,
                departureTime, departureTime, now);

        context.getVrpData().addRequest(request);

        return request;
    }


    //================ ADVANCED REQUESTS STORAGE

    private final Map<Id, Queue<PassengerRequest>> advanceRequests = new HashMap<Id, Queue<PassengerRequest>>();


    private void storeAdvancedRequest(PassengerRequest request)
    {
        MobsimAgent passenger = request.getPassenger();
        Queue<PassengerRequest> passengerAdvReqs = advanceRequests.get(passenger.getId());

        if (passengerAdvReqs == null) {
            passengerAdvReqs = new PriorityQueue<PassengerRequest>(3,
                    new Comparator<PassengerRequest>() {
                        @Override
                        public int compare(PassengerRequest r1, PassengerRequest r2)
                        {
                            return Double.compare(r1.getT0(), r2.getT0());
                        }
                    });

            advanceRequests.put(passenger.getId(), passengerAdvReqs);
        }

        passengerAdvReqs.add(request);
    }


    private PassengerRequest retrieveAdvancedRequest(MobsimAgent passenger, Id fromLinkId,
            Id toLinkId)
    {
        Queue<PassengerRequest> passengerAdvReqs = advanceRequests.get(passenger.getId());

        if (passengerAdvReqs != null) {
            PassengerRequest req = passengerAdvReqs.peek();

            if (req != null) {
                if (req.getFromLink().getId() == fromLinkId //
                        && req.getToLink().getId() == toLinkId) {
                    passengerAdvReqs.poll();
                    return req;// this is the advance request for the current leg
                }
                else {
                    if (context.getTime() > req.getT0()) {
                        //TODO we have to somehow handle it (in the future)
                        //Currently this is not a problem since we do not have such cases...
                        throw new IllegalStateException(
                                "Seems that the agent is not going to take the previously submitted request");
                    }
                }
            }
        }

        return null;//this is an immediate request
    }


    //================ WAITING FOR PASSENGERS

    //passenger's request id -> driver's stay task
    private final Map<Id, PassengerPickupActivity> awaitingPickups = new HashMap<Id, PassengerPickupActivity>();


    public void storeAwaitingPickup(PassengerRequest request, PassengerPickupActivity pickupActivity)
    {
        awaitingPickups.put(request.getId(), pickupActivity);
    }


    private PassengerPickupActivity retrieveAwaitingPickup(PassengerRequest request)
    {
        return awaitingPickups.remove(request.getId());
    }


    //================ PICKUP / DROPOFF

    public boolean pickUpPassenger(PassengerPickupActivity pickupActivity,
            MobsimDriverAgent driver, PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();
        Id linkId = driver.getCurrentLinkId();

        if (passenger.getCurrentLinkId() != linkId || passenger.getState() != State.LEG
                || !passenger.getMode().equals(mode)) {
            storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger
        }

        if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(),
                driver.getCurrentLinkId()) == null) {
            //the passenger has already been picked up and is on another taxi trip
            //seems there have been at least 2 requests made by this passenger for this location
            storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger (optimistically, he/she should appear soon)
        }

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.addPassenger(passenger);
        passenger.setVehicle(mobVehicle);

        EventsManager events = internalInterface.getMobsim().getEventsManager();
        events.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        return true;
    }


    public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.removePassenger(passenger);
        passenger.setVehicle(null);

        EventsManager events = internalInterface.getMobsim().getEventsManager();
        events.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(passenger);
    }
}
