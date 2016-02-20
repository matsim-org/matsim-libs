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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;


public class OneTaxiServeTask
    extends StayTaskImpl
{
    private final OneTaxiRequest request;
    private final boolean isPickup;//pickup or dropoff


    public OneTaxiServeTask(double beginTime, double endTime, Link link, boolean isPickup,
            OneTaxiRequest request)
    {
        super(beginTime, endTime, link, isPickup ? "pickup" : "dropoff");
        this.request = request;
        this.isPickup = isPickup;
    }


    public OneTaxiRequest getRequest()
    {
        return request;
    }


    public boolean isPickup()
    {
        return isPickup;
    }
}
