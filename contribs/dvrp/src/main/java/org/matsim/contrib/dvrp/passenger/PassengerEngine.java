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

import java.util.Map;

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
    protected final String mode;

    private EventsManager eventsManager;
    protected InternalInterface internalInterface;
    protected final MatsimVrpContext context;
    protected final PassengerRequestCreator requestCreator;
    protected final VrpOptimizer optimizer;

    protected final AdvanceRequestStorage advanceRequestStorage;
    protected final AwaitingPickupStorage awaitingPickupStorage;


    public PassengerEngine(String mode, EventsManager eventsManager,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer,
            MatsimVrpContext context)
    {
        this.mode = mode;
        this.eventsManager = eventsManager;
        this.requestCreator = requestCreator;
        this.optimizer = optimizer;
        this.context = context;

        advanceRequestStorage = new AdvanceRequestStorage(context);
        awaitingPickupStorage = new AwaitingPickupStorage();
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


    /**
     * This is to register an advance booking. The method is called when, in reality, the request is
     * made.
     * 
     * @param now -- time when trip is booked
     * @param passenger
     * @param leg -- contains information about the departure time. yyyy Michal, Joschka, note that
     *        in MATSim leg departure times may be meaningless; the only thing that truly matters is
     *        the activity end time. Is your code defensive against that? kai, jul'14 I (jb) only
     *        use this functionality after I explicitly set the Leg departure time (aug '15)
     * @return
     */
    public boolean prebookTrip(double now, MobsimPassengerAgent passenger, Leg leg)
    {
        if (!leg.getMode().equals(mode)) {
            return false;
        }

        if (leg.getDepartureTime() <= now) {
            throw new IllegalStateException("This is not a call ahead");
        }

        Id<Link> fromLinkId = leg.getRoute().getStartLinkId();
        Id<Link> toLinkId = leg.getRoute().getEndLinkId();
        double departureTime = leg.getDepartureTime();

        PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime,
                now);
        optimizer.requestSubmitted(request);

        if (!request.isRejected()) {
            advanceRequestStorage.storeAdvanceRequest(request);
        }

        return !request.isRejected();
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId)
    {
        if (!agent.getMode().equals(mode)) {
            return false;
        }

        MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;

        Id<Link> toLinkId = passenger.getDestinationLinkId();
        double departureTime = now;

        internalInterface.registerAdditionalAgentOnLink(passenger);

        PassengerRequest request = advanceRequestStorage.retrieveAdvanceRequest(passenger,
                fromLinkId, toLinkId);

        if (request == null) {//this is an immediate request
            request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
            optimizer.requestSubmitted(request);
        }
        else {
            PassengerPickupActivity awaitingPickup = awaitingPickupStorage
                    .retrieveAwaitingPickup(request);

            if (awaitingPickup != null) {
                awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
            }
        }

        return !request.isRejected();
    }


    //================ REQUESTS CREATION

    private int nextId = 0;


    protected PassengerRequest createRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId, double departureTime, double now)
    {
        Map<Id<Link>, ? extends Link> links = context.getScenario().getNetwork().getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);
        Id<Request> id = Id.create(mode + "_" + nextId++, Request.class);

        PassengerRequest request = requestCreator.createRequest(id, passenger, fromLink, toLink,
                departureTime, departureTime, now);
        context.getVrpData().addRequest(request);
        return request;
    }


    //================ PICKUP / DROPOFF

    public boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
            PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();
        Id<Link> linkId = driver.getCurrentLinkId();

        if (passenger.getCurrentLinkId() != linkId || passenger.getState() != State.LEG
                || !passenger.getMode().equals(mode)) {
            awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger
        }

        if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(),
                driver.getCurrentLinkId()) == null) {
            //the passenger has already been picked up and is on another taxi trip
            //seems there have been at least 2 requests made by this passenger for this location
            awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger (optimistically, he/she should appear soon)
        }

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.addPassenger(passenger);
        passenger.setVehicle(mobVehicle);

        eventsManager.processEvent(
                new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        return true;
    }


    public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.removePassenger(passenger);
        passenger.setVehicle(null);

        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(passenger);
    }
}
