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

package org.matsim.contrib.dvrp.vrpagent;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.dvrp.data.schedule.VrpSchedulePlanFactory;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.*;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleUtils;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;


public class VrpAgentSource
    implements AgentSource
{
    private final DynActionCreator nextActionCreator;

    private final MatsimVrpData data;
    private final VrpSimEngine vrpSimEngine;

    private final boolean isAgentWithPlan;


    public VrpAgentSource(DynActionCreator nextActionCreator, MatsimVrpData data,
            VrpSimEngine vrpSimEngine)
    {
        this(nextActionCreator, data, vrpSimEngine, false);
    }


    public VrpAgentSource(DynActionCreator nextActionCreator, MatsimVrpData data,
            VrpSimEngine vrpSimEngine, boolean isAgentWithPlan)
    {
        this.nextActionCreator = nextActionCreator;
        this.data = data;
        this.vrpSimEngine = vrpSimEngine;

        this.isAgentWithPlan = isAgentWithPlan;
    }


    @Override
    public void insertAgentsIntoMobsim()
    {
        QSim qSim = (QSim)vrpSimEngine.getInternalInterface().getMobsim();
        List<Vehicle> vehicles = data.getVrpData().getVehicles();

        for (Vehicle vrpVeh : vehicles) {
            VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(vrpSimEngine, nextActionCreator,
                    (VrpAgentVehicle)vrpVeh);

            vrpSimEngine.addAgentLogic(vrpAgentLogic);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = ((MatsimVertex)vrpVeh.getDepot().getVertex()).getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId, vrpSimEngine.getInternalInterface(),
                    vrpAgentLogic);

            if (isAgentWithPlan) {
                qSim.insertAgentIntoMobsim(new DynAgentWithPlan(taxiAgent,
                        new VrpSchedulePlanFactory(vrpVeh, data)));
            }
            else {
                qSim.insertAgentIntoMobsim(taxiAgent);
            }

            qSim.createAndParkVehicleOnLink(
                    VehicleUtils.getFactory().createVehicle(taxiAgent.getId(),
                            VehicleUtils.getDefaultVehicleType()), taxiAgent.getCurrentLinkId());
        }
    }
}
