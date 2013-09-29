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

import org.matsim.contrib.dynagent.DynAgent;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task;


public class VrpAgentVehicle
    extends VehicleImpl
{
    private VrpAgentLogic agentLogic;


    public VrpAgentVehicle(int id, String name, Depot depot, int capacity, double cost, int t0,
            int t1, int timeLimit)
    {
        super(id, name, depot, capacity, cost, t0, t1, timeLimit);
    }


    public VrpAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    public void setAgentLogic(VrpAgentLogic agentLogic)
    {
        this.agentLogic = agentLogic;
    }


    public static DynAgent getAgent(Task task)
    {
        VrpAgentVehicle vehicle = (VrpAgentVehicle)task.getSchedule().getVehicle();
        return vehicle.getAgentLogic().getDynAgent();
    }
}
