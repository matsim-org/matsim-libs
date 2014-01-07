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

package org.matsim.contrib.dvrp.data.network;

import org.matsim.api.core.v01.network.Link;


public class VrpPathImpl
    implements VrpPath
{
    private final int departureTime;
    private final int travelTime;
    private final double travelCost;
    private final Link[] links;
    private final int[] accLinkTravelTimes;//accumulated link travel times


    public VrpPathImpl(int departureTime, int travelTime, double travelCost, Link[] links,
            int[] accLinkTravelTimes)
    {
        this.departureTime = departureTime;
        this.travelTime = travelTime;
        this.travelCost = travelCost;
        this.links = links;
        this.accLinkTravelTimes = accLinkTravelTimes;
    }


    @Override
    public int getDepartureTime()
    {
        return departureTime;
    }


    @Override
    public int getTravelTime()
    {
        return travelTime;
    }


    @Override
    public int getArrivalTime()
    {
        return departureTime + travelTime;
    }


    @Override
    public double getTravelCost()
    {
        return travelCost;
    }


    @Override
    public Link[] getLinks()
    {
        return links;
    }


    @Override
    public Link getFromLink()
    {
        return links[0];
    }


    @Override
    public Link getToLink()
    {
        return links[links.length - 1];
    }
    
    
    public int[] getAccLinkTravelTimes()
    {
        return accLinkTravelTimes;
    }
}
