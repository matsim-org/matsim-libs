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

package playground.michalm.vrp.data.model;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.dynamic.DynAgentLogic;


public class DynVehicle
    extends VehicleImpl
{
    private DynAgentLogic agentLogic;


    public DynVehicle(int id, String name, Depot depot, int capacity, double cost, int t0, int t1,
            int timeLimit)
    {
        super(id, name, depot, capacity, cost, t0, t1, timeLimit);
    }


    public DynAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    public void setAgentLogic(DynAgentLogic agentLogic)
    {
        this.agentLogic = agentLogic;
    }
}
