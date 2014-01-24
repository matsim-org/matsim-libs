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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.model.impl.VehicleImpl;


public class VrpAgentVehicleImpl
    extends VehicleImpl
    implements VrpAgentVehicle
{
    private VrpAgentLogic agentLogic;


    public VrpAgentVehicleImpl(Id id, String name, Link startLink, double capacity, double t0,
            double t1, double timeLimit)
    {
        super(id, name, startLink, capacity, t0, t1, timeLimit);
    }


    @Override
    public VrpAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    @Override
    public void setAgentLogic(VrpAgentLogic agentLogic)
    {
        this.agentLogic = agentLogic;
    }
}
