/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.*;

import com.google.inject.Inject;


/**
 * Used for benchmarking, i.e. when we want to run several identical iterations, i.e. day-to-day
 * learning/adaptation is turned off and link travel times are taken from an external source (i.e.
 * not from the previous iteration).
 */
public class InactiveTravelTimeCalculator
    extends TravelTimeCalculator
{
    private final TravelTime travelTime = new FreeSpeedTravelTime();
    private final Network network;


    @Inject
    public InactiveTravelTimeCalculator(Network network,
            TravelTimeCalculatorConfigGroup ttconfigGroup)
    {
        super(network, ttconfigGroup);
        this.network = network;

        if (ttconfigGroup.isCalculateLinkToLinkTravelTimes()) {
            throw new IllegalArgumentException("Use this class with link travel times");
        }
    }


    @Override
    public TravelTime getLinkTravelTimes()
    {
        return travelTime;
    }


    @Override
    public double getLinkTravelTime(Id<Link> linkId, double time)
    {
        return travelTime.getLinkTravelTime(network.getLinks().get(linkId), time, null, null);
    }


    @Override
    public double getLinkToLinkTravelTime(Id<Link> fromLinkId, Id<Link> toLinkId, double time)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public LinkToLinkTravelTime getLinkToLinkTravelTimes()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(LinkEnterEvent e)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(LinkLeaveEvent e)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(VehicleEntersTrafficEvent event)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void handleEvent(VehicleAbortsEvent event)
    {
        throw new UnsupportedOperationException();
    }
}
