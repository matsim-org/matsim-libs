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

package org.matsim.contrib.dynagent.examples.random;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.*;


public class RandomDynAgentSource
    implements AgentSource
{
    private final QSim qSim;
    private final int agentCount;


    public RandomDynAgentSource(QSim qSim, int agentCount)
    {
        this.qSim = qSim;
        this.agentCount = agentCount;
    }


    @Override
    public void insertAgentsIntoMobsim()
    {
        Scenario scenario = qSim.getScenario();
        Network network = scenario.getNetwork();
        VehiclesFactory qSimVehicleFactory = VehicleUtils.getFactory();

        for (int i = 0; i < agentCount; i++) {
            RandomDynAgentLogic agentLogic = new RandomDynAgentLogic(network);

            Id<Person> id = Id.createPersonId(i);
            Id<Link> startLinkId = RandomDynAgentLogic
                    .chooseRandomElement(network.getLinks().keySet());
            DynAgent agent = new DynAgent(id, startLinkId, qSim.getEventsManager(), agentLogic);

            qSim.createAndParkVehicleOnLink(
                    qSimVehicleFactory.createVehicle(Id.create(id, Vehicle.class),
                            VehicleUtils.getDefaultVehicleType()),
                    startLinkId);
            qSim.insertAgentIntoMobsim(agent);
        }
    }
}
