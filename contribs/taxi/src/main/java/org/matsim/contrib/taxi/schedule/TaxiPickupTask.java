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

package org.matsim.contrib.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.taxi.data.TaxiRequest;


public class TaxiPickupTask
    extends StayTaskImpl
    implements TaxiTaskWithRequest
{
    private final TaxiRequest request;


    public TaxiPickupTask(double beginTime, double endTime, TaxiRequest request)
    {
        super(beginTime, endTime, request.getFromLink());

        this.request = request;
        request.setPickupTask(this);
    }


    @Override
    public void removeFromRequest()
    {
        request.setPickupTask(null);
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.PICKUP;
    }


    public TaxiRequest getRequest()
    {
        return request;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
