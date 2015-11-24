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

package playground.michalm.taxi.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.run.LazyDynActivityEngine;


public class TaxiStayTask
    extends StayTaskImpl
    implements TaxiTask
{
    private final DynAgent dynAgent;
    private LazyDynActivityEngine lazyDynActivityEngine;


    public TaxiStayTask(double beginTime, double endTime, Link link)
    {
        super(beginTime, endTime, link);
        this.dynAgent = getSchedule().getVehicle().getAgentLogic().getDynAgent();
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.STAY;
    }


    @Override
    public void setEndTime(double endTime)
    {
        double oldEndTime = getEndTime();
        if (endTime != oldEndTime) {
            super.setEndTime(endTime);

            if (getStatus() == TaskStatus.STARTED && lazyDynActivityEngine != null) {
                lazyDynActivityEngine.rescheduleDynActivity(dynAgent);
            }
        }
    }


    public void setLazyDynActivityEngine(LazyDynActivityEngine lazyDynActivityEngine)
    {
        this.lazyDynActivityEngine = lazyDynActivityEngine;
    }
    

    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
