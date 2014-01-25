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

import java.util.*;

import org.matsim.contrib.dvrp.data.model.*;
import org.matsim.core.mobsim.framework.MobsimTimer;


/**
 * @author michalm
 */
public class VrpDataImpl
    implements VrpData
{
    private final List<Vehicle> vehicles = new ArrayList<Vehicle>();
    private final List<Request> requests = new ArrayList<Request>();

    private MobsimTimer mobsimTimer;


    @Override
    public List<Vehicle> getVehicles()
    {
        return vehicles;
    }


    @Override
    public List<Request> getRequests()
    {
        return requests;
    }


    @Override
    public double getTime()
    {
        return mobsimTimer.getTimeOfDay();
    }


    // SETTERS
    /*package*/void setMobsimTimer(MobsimTimer mobsimTimer)
    {
        this.mobsimTimer = mobsimTimer;
    }
}
