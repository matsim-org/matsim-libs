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

package org.matsim.contrib.dvrp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.mobsim.framework.MobsimTimer;


public class MatsimVrpContextImpl
    implements MatsimVrpContext
{
    private VrpData vrpData;
    private Scenario scenario;
    private MobsimTimer timer;


    @Override
    public VrpData getVrpData()
    {
        return vrpData;
    }


    @Override
    public Scenario getScenario()
    {
        return scenario;
    }


    @Override
    public double getTime()
    {
        return timer.getTimeOfDay();
    }


    public void setVrpData(VrpData vrpData)
    {
        this.vrpData = vrpData;
    }


    public void setScenario(Scenario scenario)
    {
        this.scenario = scenario;
    }


    public void setMobsimTimer(MobsimTimer timer)
    {
        this.timer = timer;
    }
}
