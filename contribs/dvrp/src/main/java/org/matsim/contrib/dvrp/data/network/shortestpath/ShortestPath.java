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

package org.matsim.contrib.dvrp.data.network.shortestpath;

import org.matsim.api.core.v01.network.Link;


public class ShortestPath
{
    public final int travelTime;
    public final double travelCost;
    public final Link[] links;
    public final int[] accLinkTravelTimes;//accumulated link travel times


    public ShortestPath(int travelTime, double travelCost, Link[] links, int[] accLinkTravelTimes)
    {
        this.travelTime = travelTime;
        this.travelCost = travelCost;
        this.links = links;
        this.accLinkTravelTimes = accLinkTravelTimes;
    }
}
