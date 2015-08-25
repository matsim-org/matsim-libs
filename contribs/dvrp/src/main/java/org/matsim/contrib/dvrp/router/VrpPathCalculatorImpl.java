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

package org.matsim.contrib.dvrp.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class VrpPathCalculatorImpl
    implements VrpPathCalculator
{
    private final LeastCostPathCalculator router;
    private final TravelTime travelTime;
    private final TravelDisutility travelDisutility;


    public VrpPathCalculatorImpl(LeastCostPathCalculator router, TravelTime travelTime,
            TravelDisutility travelDisutility)
    {
        this.router = router;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
    }


    /**
     * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
     */
    @Override
    public VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime)
    {
        if (fromLink == toLink) {
            return new VrpPathImpl(departureTime, 0, 0, new Link[] { fromLink },
                    new double[] { 0 });
        }

        //calc path for departureTime+1 (we need 1 second to move over the node)
        Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                departureTime + 1, null, null);

        int count = path.links.size();
        Link[] links = new Link[count + 2];
        double[] linkTT = new double[count + 2];

        //we start at the end of fromLink
        //actually, in QSim, it usually takes 1 second to move over the first node
        //(when INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES is ON;
        //otherwise it can take much longer)
        double currentTime = departureTime;
        links[0] = fromLink;
        double tt = 1.;
        linkTT[0] = tt;
        currentTime += tt;

        for (int i = 1; i <= count; i++) {
            Link link = path.links.get(i - 1);
            links[i] = link;
            tt = travelTime.getLinkTravelTime(link, currentTime, null, null);
            linkTT[i] = tt;
            currentTime += tt;
        }

        //there is no extra time spent on queuing at the end of the last link
        links[count + 1] = toLink;
        tt = toLink.getLength() / toLink.getFreespeed();///???????????????
        linkTT[count + 1] = tt;

        double totalTT = 1 + path.travelTime + tt;

        double totalCost = path.travelCost
                + travelDisutility.getLinkMinimumTravelDisutility(toLink);

        return new VrpPathImpl(departureTime, totalTT, totalCost, links, linkTT);
    }
}
