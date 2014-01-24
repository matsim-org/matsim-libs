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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.model.Request;
import org.matsim.contrib.dvrp.data.schedule.Task;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;


public class PassengerEngine
    implements MobsimEngine, DepartureHandler

{
    private final String mode;

    private InternalInterface internalInterface;
    private final MatsimVrpData data;
    private final PassengerRequestCreator requestCreator;
    private final VrpOptimizer optimizer;


    public PassengerEngine(String mode, PassengerRequestCreator requestCreator,
            VrpOptimizer optimizer, MatsimVrpData data)
    {
        this.mode = mode;
        this.requestCreator = requestCreator;
        this.optimizer = optimizer;
        this.data = data;
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
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


    private final Map<Id, Set<PassengerRequest>> advanceRequests = new HashMap<Id, Set<PassengerRequest>>();


    public boolean callAhead(double now, MobsimAgent passenger, Leg leg)
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

        //==============================================

        PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime,
                now);

        Set<PassengerRequest> passengerAdvReqs = advanceRequests.get(passenger.getId());
        if (passengerAdvReqs == null) {
            passengerAdvReqs = new TreeSet<PassengerRequest>(new Comparator<PassengerRequest>() {
                @Override
                public int compare(PassengerRequest r1, PassengerRequest r2)
                {
                    return Double.compare(r1.getT0(), r2.getT0());
                }
            });
            
            advanceRequests.put(passenger.getId(), passengerAdvReqs);
        }

        passengerAdvReqs.add(request);
        optimizer.requestSubmitted(request);
        return true;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent passenger, Id fromLinkId)
    {
        if (!passenger.getMode().equals(mode)) {
            return false;
        }

        Id toLinkId = passenger.getDestinationLinkId();
        double departureTime = now;

        //==============================================

        //check if this is an advance request
        Set<PassengerRequest> passengerAdvReqs = advanceRequests.get(passenger.getId());
        PassengerRequest request = null;

        if (passengerAdvReqs != null) {
            for (PassengerRequest r : passengerAdvReqs) {
                if (r.getFromLink().getId() == fromLinkId //
                        && r.getToLink().getId() == toLinkId) {
                    //there may be more than one request for the (fromLinkId, toLinkId) pair
                    //this one, however, is the earliest one (lowest req.T0)
                    request = r;

                    //remove it from TreeSet
                    passengerAdvReqs.remove(r);
                    break;
                }
            }
        }

        if (request == null) {//this is an immediate request
            request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
        }

        internalInterface.registerAdditionalAgentOnLink(passenger);
        optimizer.requestSubmitted(request);
        return true;
    }


    private PassengerRequest createRequest(MobsimAgent passenger, Id fromLinkId, Id toLinkId,
            double departureTime, double now)
    {
        Map<Id, ? extends Link> links = data.getScenario().getNetwork().getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);

        List<Request> requests = data.getVrpData().getRequests();
        Id id = data.getScenario().createId(requests.size() + "");

        PassengerRequest request = requestCreator.createRequest(id, passenger, fromLink, toLink,
                departureTime, departureTime, now);

        requests.add(request);

        return request;
    }


    public void pickUpPassenger(Task task, PassengerRequest request, double now)
    {
        DriverAgent driver = VrpAgents.getAgent(task);
        MobsimAgent passenger = request.getPassenger();

        Id currentLinkId = passenger.getCurrentLinkId();

        if (currentLinkId != driver.getCurrentLinkId()) {
            throw new IllegalStateException("Passenger and vehicle on different links!");
        }

        if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for vehicle");
        }

        EventsManager events = internalInterface.getMobsim().getEventsManager();
        events.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), driver
                .getVehicle().getId()));

        if (passenger instanceof PassengerAgent) {
            PassengerAgent passengerAgent = (PassengerAgent)passenger;
            MobsimVehicle mobVehicle = driver.getVehicle();
            mobVehicle.addPassenger(passengerAgent);
            passengerAgent.setVehicle(mobVehicle);
        }
        else {
            Logger.getLogger(PassengerEngine.class).warn(
                    "mobsim agent could not be converted to type PassengerAgent; will probably work anyway but "
                            + "for the simulation the agent is now not in the vehicle");
        }
    }


    public void dropOffPassenger(Task task, PassengerRequest request, double now)
    {
        DriverAgent driver = VrpAgents.getAgent(task);
        MobsimAgent passenger = request.getPassenger();

        if (passenger instanceof PassengerAgent) {
            PassengerAgent passengerAgent = (PassengerAgent)passenger;
            MobsimVehicle mobVehicle = driver.getVehicle();
            mobVehicle.removePassenger(passengerAgent);
            passengerAgent.setVehicle(null);
        }

        EventsManager events = internalInterface.getMobsim().getEventsManager();
        events.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), driver
                .getVehicle().getId()));

        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(passenger);
    }
}
