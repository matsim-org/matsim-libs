/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

import pl.poznan.put.vrp.dynamic.data.schedule.Task;


public class PassengerHandlingUtils
{
    public static void pickUpPassenger(VrpSimEngine vrpSimEngine, Task task,
            PassengerRequest request, double now)
    {
        DriverAgent driver = VrpAgents.getAgent(task);
        pickUpPassenger(vrpSimEngine, driver, request.getPassengerAgent(), now);
    }


    public static void pickUpPassenger(VrpSimEngine vrpSimEngine, DriverAgent driver,
            MobsimAgent passenger, double now)
    {
        Id currentLinkId = passenger.getCurrentLinkId();

        if (currentLinkId != driver.getCurrentLinkId()) {
            throw new IllegalStateException("Passenger and vehicle on different links!");
        }

        if (vrpSimEngine.getInternalInterface().unregisterAdditionalAgentOnLink(passenger.getId(),
                currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for vehicle");
        }

        EventsManager events = vrpSimEngine.getInternalInterface().getMobsim().getEventsManager();
        events.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), driver
                .getVehicle().getId()));

        if (passenger instanceof PassengerAgent) {
            PassengerAgent passengerAgent = (PassengerAgent)passenger;
            MobsimVehicle mobVehicle = driver.getVehicle();
            mobVehicle.addPassenger(passengerAgent);
            passengerAgent.setVehicle(mobVehicle);
        }
        else {
            Logger.getLogger(PassengerHandlingUtils.class).warn(
                    "mobsim agent could not be converted to type PassengerAgent; will probably work anyway but "
                            + "for the simulation the agent is now not in the vehicle");
        }
    }


    public static void dropOffPassenger(VrpSimEngine vrpSimEngine, Task task,
            PassengerRequest request, double now)
    {
        DriverAgent driver = VrpAgents.getAgent(task);
        PassengerHandlingUtils.dropOffPassenger(vrpSimEngine, driver, request.getPassengerAgent(),
                now);
    }


    public static void dropOffPassenger(VrpSimEngine vrpSimEngine, DriverAgent driver,
            MobsimAgent passenger, double now)
    {
        if (passenger instanceof PassengerAgent) {
            PassengerAgent passengerAgent = (PassengerAgent)passenger;
            MobsimVehicle mobVehicle = driver.getVehicle();
            mobVehicle.removePassenger(passengerAgent);
            passengerAgent.setVehicle(null);
        }

        EventsManager events = vrpSimEngine.getInternalInterface().getMobsim().getEventsManager();
        events.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), driver
                .getVehicle().getId()));

        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        vrpSimEngine.getInternalInterface().arrangeNextAgentState(passenger);
    }
}
