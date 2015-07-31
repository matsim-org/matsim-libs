/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;


public class VehicleImpl
    implements Vehicle
{
    private final Id<Vehicle> id;
    private final Link startLink;
    private final double capacity;

    // TW for vehicle
    private final double t0;
    private double t1;

    private Schedule<? extends AbstractTask> schedule;

    private VrpAgentLogic agentLogic;


    public VehicleImpl(Id<Vehicle> id, Link startLink, double capacity, double t0, double t1)
    {
        this.id = id;
        this.startLink = startLink;
        this.capacity = capacity;
        this.t0 = t0;
        this.t1 = t1;

        schedule = new ScheduleImpl<AbstractTask>(this);
    }


    @Override
    public Id<Vehicle> getId()
    {
        return id;
    }


    @Override
    public Link getStartLink()
    {
        return startLink;
    }


    @Override
    public double getCapacity()
    {
        return capacity;
    }


    @Override
    public double getT0()
    {
        return t0;
    }


    @Override
    public double getT1()
    {
        return t1;
    }


    @Override
    public Schedule<? extends AbstractTask> getSchedule()
    {
        return schedule;
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


    @Override
    public String toString()
    {
        return "Vehicle_" + id;
    }


    @Override
    public void setT1(double t1)
    {
        this.t1 = t1;
    }
    
    public void resetSchedule(){
   	 schedule = new ScheduleImpl<AbstractTask>(this);
   }
}
