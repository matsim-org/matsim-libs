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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;


public class PickupDeliveryTaskImpl
    extends StayTaskImpl
    implements PickupDeliveryTask
{
    private final boolean pickup;

    private final List<PickupDeliveryRequest> requests = new ArrayList<>();
    private final List<PickupDeliveryRequest> unmodifiableRequests = Collections
            .unmodifiableList(requests);


    public PickupDeliveryTaskImpl(double beginTime, double endTime, Link link, boolean pickup)
    {
        super(beginTime, endTime, link);
        this.pickup = pickup;
    }


    @Override
    public boolean isPickup()
    {
        return pickup;
    }


    @Override
    public List<PickupDeliveryRequest> getRequests()
    {
        return unmodifiableRequests;
    }


    @Override
    public void addRequest(PickupDeliveryRequest request)
    {
        if ( (pickup && request.getFromLink() != getLink()) //
                || (!pickup && request.getToLink() != getLink())) {
            throw new RuntimeException();
        }

        requests.add(request);

        if (pickup) {
            request.setPickupTask(this);
        }
        else {
            request.setDeliveryTask(this);
        }
    }


    @Override
    public void removeRequest(PickupDeliveryRequest request)
    {
        removeFromRequest(request);
        requests.remove(request);
    }


    @Override
    public void removeAllRequests()
    {
        for (PickupDeliveryRequest r : requests) {
            removeFromRequest(r);
        }

        requests.clear();
    }


    private void removeFromRequest(PickupDeliveryRequest request)
    {
        if (pickup) {
            request.setPickupTask(null);
        }
        else {
            request.setDeliveryTask(null);
        }
    }
}
