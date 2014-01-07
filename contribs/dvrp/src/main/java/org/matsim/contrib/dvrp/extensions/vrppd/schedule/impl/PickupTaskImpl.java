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

package org.matsim.contrib.dvrp.extensions.vrppd.schedule.impl;

import org.matsim.contrib.dvrp.data.schedule.impl.StayTaskImpl;
import org.matsim.contrib.dvrp.extensions.vrppd.model.PickupRequest;
import org.matsim.contrib.dvrp.extensions.vrppd.schedule.PickupTask;


public class PickupTaskImpl
    extends StayTaskImpl
    implements PickupTask
{
    private final PickupRequest request;


    public PickupTaskImpl(int beginTime, int endTime, PickupRequest request)
    {
        super(beginTime, endTime, request.getFromLink());
        this.request = request;
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
