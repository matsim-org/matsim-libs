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

package org.matsim.contrib.dvrp.data;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.passenger.PassengerCustomer;
import org.matsim.core.mobsim.framework.MobsimAgent;


/**
 * Contains mappings: MobsimAgent.id => xxxx
 */
public class MobsimAgentMappings
{
    private final Map<Id, MobsimAgent> mobsimAgents = new HashMap<Id, MobsimAgent>();

    private final Map<Id, PassengerCustomer> passengerCustomers = new HashMap<Id, PassengerCustomer>();


    public Map<Id, MobsimAgent> getMobsimAgents()
    {
        return mobsimAgents;
    }


    public Map<Id, PassengerCustomer> getPassengerCustomers()
    {
        return passengerCustomers;
    }
}
