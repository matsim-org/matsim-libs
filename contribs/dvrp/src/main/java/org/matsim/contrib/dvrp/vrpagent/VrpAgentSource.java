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
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.util.schedule.VrpSchedulePlanFactory;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.util.DynAgentWithPlan;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.*;


public class VrpAgentSource
    implements AgentSource
{
    private final DynActionCreator nextActionCreator;

    private final MatsimVrpData data;
    private final VrpOptimizer optimizer;
    private final QSim qSim;

    private final boolean isAgentWithPlan;


    public VrpAgentSource(DynActionCreator nextActionCreator, MatsimVrpData data,
            VrpOptimizer optimizer, QSim qSim)
    {
        this(nextActionCreator, data, optimizer, qSim, false);
    }


    public VrpAgentSource(DynActionCreator nextActionCreator, MatsimVrpData data,
            VrpOptimizer optimizer, QSim qSim, boolean isAgentWithPlan)
    {
        this.nextActionCreator = nextActionCreator;
        this.data = data;
        this.optimizer = optimizer;
        this.qSim = qSim;
        this.isAgentWithPlan = isAgentWithPlan;
    }


    @Override
    public void insertAgentsIntoMobsim()
    {
        List<Vehicle> vehicles = data.getVrpData().getVehicles();
        VehiclesFactory qSimVehicleFactory = VehicleUtils.getFactory();

        for (Vehicle vrpVeh : vehicles) {
            VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator,
                    (VrpAgentVehicle)vrpVeh);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = vrpVeh.getDepot().getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId, qSim, vrpAgentLogic);

            if (isAgentWithPlan) {
                qSim.insertAgentIntoMobsim(new DynAgentWithPlan(taxiAgent,
                        new VrpSchedulePlanFactory(vrpVeh, data)));
            }
            else {
                qSim.insertAgentIntoMobsim(taxiAgent);
            }

            qSim.createAndParkVehicleOnLink(
                    qSimVehicleFactory.createVehicle(id, VehicleUtils.getDefaultVehicleType()),
                    startLinkId);
        }
    }
}
