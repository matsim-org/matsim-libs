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

package org.matsim.contrib.dvrp.extensions.vrppd;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;


public class PickupTaskImpl
    extends StayTaskImpl
    implements PickupTask
{
    private final PickupRequest request;


    public PickupTaskImpl(double beginTime, double endTime, PickupRequest request)
    {
        super(beginTime, endTime, request.getFromLink());
        this.request = request;
        request.setPickupTask(this);
    }


    @Override
    public VRPPDTaskType getVRPPDTaskType()
    {
        return VRPPDTaskType.PICKUP_STAY;
    }


    @Override
    public PickupRequest getRequest()
    {
        return request;
    }


    @Override
    public void removeFromRequest()
    {
        request.setPickupTask(null);
    }
}
