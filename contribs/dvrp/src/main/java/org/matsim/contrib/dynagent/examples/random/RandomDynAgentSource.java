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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;


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

            Id<DynAgent> id = Id.create(i, DynAgent.class);
            Id<Link> startLinkId = RandomDynAgentLogic.chooseRandomElement(network.getLinks().keySet());
            DynAgent agent = new DynAgent(id, startLinkId, qSim, agentLogic);

            qSim.createAndParkVehicleOnLink(
                    qSimVehicleFactory.createVehicle(id, VehicleUtils.getDefaultVehicleType()),
                    startLinkId);
            qSim.insertAgentIntoMobsim(agent);
        }
    }
}
