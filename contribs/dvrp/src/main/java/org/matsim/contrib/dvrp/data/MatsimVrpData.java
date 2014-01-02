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

package org.matsim.contrib.dvrp.data;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.passenger.PassengerCustomer;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import pl.poznan.put.vrp.dynamic.data.VrpData;


public class MatsimVrpData
{
    private final VrpData vrpData;
    private final Scenario scenario;

    //all mobsim agents
    private final Map<Id, MobsimAgent> mobsimAgents = new HashMap<Id, MobsimAgent>();
    
    //TODO this is not very general -- some VRPs may be about commodities, not persons
    //passenger agents --> customers
    private final Map<Id, PassengerCustomer> customersByAgentId = new HashMap<Id, PassengerCustomer>();

    private final String coordSystem;


    public MatsimVrpData(VrpData vrpData, Scenario scenario)
    {
        this(vrpData, scenario, TransformationFactory.WGS84_UTM33N);
    }


    public MatsimVrpData(VrpData vrpData, Scenario scenario, String coordSystem)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;
        this.coordSystem = coordSystem;
    }


    public VrpData getVrpData()
    {
        return vrpData;
    }


    public Scenario getScenario()
    {
        return scenario;
    }


    public String getCoordSystem()
    {
        return coordSystem;
    }


    public Map<Id, MobsimAgent> getMobsimAgents()
    {
        return mobsimAgents;
    }


    public Map<Id, PassengerCustomer> getCustomersByAgentId()
    {
        return customersByAgentId;
    }
}
