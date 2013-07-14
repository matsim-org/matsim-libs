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

package playground.michalm.vrp.taxi;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleUtils;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.model.DynAgentVehicle;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.driver.VrpSchedulePlanFactory;


public class TaxiAgentSource
    implements AgentSource
{
    private MatsimVrpData data;
    private TaxiSimEngine taxiSimEngine;
    private boolean isAgentWithPlan;


    public TaxiAgentSource(MatsimVrpData data, TaxiSimEngine vrpSimEngine)
    {
        this(data, vrpSimEngine, false);
    }


    public TaxiAgentSource(MatsimVrpData data, TaxiSimEngine taxiSimEngine, boolean isAgentWithPlan)
    {
        this.data = data;
        this.taxiSimEngine = taxiSimEngine;
        this.isAgentWithPlan = isAgentWithPlan;
    }


    @Override
    public void insertAgentsIntoMobsim()
    {
        QSim qSim = (QSim)taxiSimEngine.getInternalInterface().getMobsim();
        List<Vehicle> vehicles = data.getVrpData().getVehicles();

        for (Vehicle vrpVeh : vehicles) {
            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(vrpVeh, taxiSimEngine,
                    data.getMatsimVrpGraph());
            taxiSimEngine.addAgentLogic(taxiAgentLogic);

            ((DynAgentVehicle)vrpVeh).setAgentLogic(taxiAgentLogic);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = ((MatsimVertex)vrpVeh.getDepot().getVertex()).getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId,
                    taxiSimEngine.getInternalInterface(), taxiAgentLogic);

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
