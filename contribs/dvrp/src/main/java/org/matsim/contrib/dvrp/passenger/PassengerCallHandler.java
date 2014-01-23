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

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;


//TODO this class is not finished yet
public class PassengerCallHandler
    implements ActivityStartEventHandler
{
    private final String type;
    private final PassengerRequestCreator requestCreator;
    private final MatsimVrpData data;
    private final VrpOptimizer optimizer;


    public PassengerCallHandler(String type, PassengerRequestCreator requestCreator,
            VrpOptimizer optimizer, MatsimVrpData data)
    {
        this.type = type;
        this.requestCreator = requestCreator;
        this.optimizer = optimizer;
        this.data = data;
    }


    @Override
    public void handleEvent(ActivityStartEvent event)
    {
        if (!event.getActType().equals(type)) {
            return;
        }

//        Network network = data.getScenario().getNetwork();
//        Link link = network.getLinks().get(event.getLinkId());
//
//        MobsimAgent passenger = data.getMobsimAgentMappings().getMobsimAgents()
//                .get(event.getPersonId());
//
//        double serveTime = passenger.getActivityEndTime();//TODO is this the best idea???
//        //can we get the serveTime in any other way?
//        //what about the destination?
//
//        PassengerCustomer customer = PassengerCustomer
//                .getOrCreatePassengerCustomer(data, passenger);
//        Request request = requestCreator.createRequest(customer, link, null, serveTime);
//
//        optimizer.requestSubmitted(request);
    }


    @Override
    public void reset(int iteration)
    {}
}
