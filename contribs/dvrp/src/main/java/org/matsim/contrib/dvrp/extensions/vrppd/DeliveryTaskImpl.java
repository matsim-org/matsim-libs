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


public class DeliveryTaskImpl
    extends StayTaskImpl
    implements DeliveryTask
{
    private final DeliveryRequest request;


    public DeliveryTaskImpl(double beginTime, double endTime, DeliveryRequest request)
    {
        super(beginTime, endTime, request.getToLink());
        this.request = request;
        this.request.setDeliveryTask(this);
    }


    @Override
    public VRPPDTaskType getVRPPDTaskType()
    {
        return VRPPDTaskType.DELIVERY_STAY;
    }


    @Override
    public DeliveryRequest getRequest()
    {
        return request;
    }


    @Override
    public void removeFromRequest()
    {
        request.setDeliveryTask(null);
    }
}
