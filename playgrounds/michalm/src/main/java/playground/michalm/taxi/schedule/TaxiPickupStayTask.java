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

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.StayTaskImpl;


public class TaxiPickupStayTask
    extends StayTaskImpl
    implements TaxiTask
{
    private final Request request;


    public TaxiPickupStayTask(int beginTime, int endTime, Request request)
    {
        super(beginTime, endTime, request.getFromVertex());
        this.request = request;
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.PICKUP_STAY;
    }


    public Request getRequest()
    {
        return request;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
