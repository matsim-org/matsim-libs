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

package org.matsim.contrib.dvrp.path;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;

import com.google.common.collect.Iterators;


public class VrpPathWithTravelDataImpl
    implements VrpPathWithTravelData
{
    private final double departureTime;
    private final double travelTime;
    private final double travelCost;
    private final Link[] links;
    private final double[] linkTTs;//accumulated link travel times


    public VrpPathWithTravelDataImpl(double departureTime, double travelTime, double travelCost, Link[] links,
            double[] linkTTs)
    {
        if (links.length == 0 || links.length != linkTTs.length) {
            throw new IllegalArgumentException();
        }

        this.departureTime = departureTime;
        this.travelTime = travelTime;
        this.travelCost = travelCost;
        this.links = links;
        this.linkTTs = linkTTs;
    }


    @Override
    public double getDepartureTime()
    {
        return departureTime;
    }


    @Override
    public double getTravelTime()
    {
        return travelTime;
    }


    @Override
    public double getArrivalTime()
    {
        return departureTime + travelTime;
    }


    @Override
    public double getTravelCost()
    {
        return travelCost;
    }


    @Override
    public int getLinkCount()
    {
        return links.length;
    }


    @Override
    public Link getLink(int idx)
    {
        return links[idx];
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


    @Override
    public double getLinkTravelTime(int idx)
    {
        return linkTTs[idx];
    }


    @Override
    public Iterator<Link> iterator()
    {
        return Iterators.forArray(links);
    }

    @Override
	public Link[] getLinks() {
		return links;
	}

    @Override
	public double[] getLinkTTs() {
		return linkTTs;
	}
    
}
