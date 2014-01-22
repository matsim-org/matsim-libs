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
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.model.Customer;
import org.matsim.contrib.dvrp.data.schedule.Task;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.interfaces.*;


public class PassengerEngine
    implements MobsimEngine, DepartureHandler

{
    private final Map<Id, MobsimAgent> mobsimAgents = new HashMap<Id, MobsimAgent>();
    private final Map<Id, PassengerCustomer> passengerCustomers = new HashMap<Id, PassengerCustomer>();

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
    {
        QSim qSim = (QSim)internalInterface.getMobsim();

        for (MobsimAgent mobsimAgent : qSim.getAgents()) {
            mobsimAgents.put(mobsimAgent.getId(), mobsimAgent);
        }
    }


    @Override
    public void doSimStep(double time)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void afterSim()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent passengerAgent, Id fromLinkId)
    {
        if (!passengerAgent.getMode().equals(mode)) {
            return false;
        }

        internalInterface.registerAdditionalAgentOnLink(passengerAgent);
        Id toLinkId = passengerAgent.getDestinationLinkId();

        Map<Id, ? extends Link> links = data.getScenario().getNetwork().getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);

        PassengerCustomer customer = getOrCreatePassengerCustomer(passengerAgent);
        List<PassengerRequest> submittedReqs = customer.getRequests();

        for (PassengerRequest r : submittedReqs) {
            if (r.getFromLink() == fromLink && r.getToLink() == toLink && r.getT0() <= now
                    && r.getT1() + 1 >= now) {
                //This is it! This is an advance request, so do not resubmit a duplicate!
                return true;
            }
        }

        PassengerRequest request = requestCreator.createRequest(customer, fromLink, toLink, now);
        submittedReqs.add(request);
        optimizer.requestSubmitted(request);

        return true;
    }


    public void pickUpPassenger(Task task, PassengerRequest request, double now)
    {
        DriverAgent driver = VrpAgents.getAgent(task);
        MobsimAgent passenger = request.getPassengerAgent();

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
        MobsimAgent passenger = request.getPassengerAgent();

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


    private PassengerCustomer getOrCreatePassengerCustomer(MobsimAgent passenger)
    {
        PassengerCustomer customer = passengerCustomers.get(passenger.getId());

        if (customer == null) {
            List<Customer> customers = data.getVrpData().getCustomers();
            customer = new PassengerCustomer(passenger);
            customers.add(customer);
        }

        return customer;
    }
}
