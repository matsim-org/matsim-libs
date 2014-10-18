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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;


public class PickupRequestImpl
    extends RequestImpl
    implements PickupRequest
{
    private final Link fromLink;
    private PickupTask pickupTask;


    public PickupRequestImpl(Id<Request> id, double quantity, double t0, double t1,
            double submissionTime, Link fromLink)
    {
        super(id, quantity, t0, t1, submissionTime);
        this.fromLink = fromLink;
    }


    @Override
    public RequestType getRequestType()
    {
        return RequestType.PICKUP;
    }


    @Override
    public Link getFromLink()
    {
        return fromLink;
    }


    @Override
    public PickupTask getPickupTask()
    {
        return pickupTask;
    }


    public void setPickupTask(PickupTask pickupTask)
    {
        this.pickupTask = pickupTask;
    }
}
