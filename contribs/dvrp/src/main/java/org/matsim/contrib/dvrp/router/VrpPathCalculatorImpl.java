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
    public VrpPath calcPath(Link fromLink, Link toLink, int departureTime)
    {
        if (fromLink != toLink) {
            Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departureTime, null, null);

            int count = path.links.size();
            Link[] links = new Link[count + 2];
            int[] accLinkTravelTimes = new int[count + 2];

            links[0] = fromLink;
            double accTT = 1.;//we start at the end of fromLink
            //actually, in QSim, it usually takes 1 second to move over the first node
            //(when INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES is ON;
            //otherwise it may take even much longer)
            accLinkTravelTimes[0] = (int)accTT;

            for (int i = 1; i <= count; i++) {
                Link link = path.links.get(i - 1);
                links[i] = link;
                accTT += travelTime.getLinkTravelTime(link, departureTime + accTT, null, null);
                accLinkTravelTimes[i] = (int)accTT;
            }

            
            //TODO there is extra time spent on queuing at the end of the last link - the vehicle stops there
            
            //?
            //?
            //?
            //?
            
            links[count + 1] = toLink;
            int toLinkEnterTime = departureTime + (int)accTT;
            accTT += travelTime.getLinkTravelTime(toLink, toLinkEnterTime, null, null);
            accLinkTravelTimes[count + 1] = (int)accTT;

            double cost = path.travelCost
                    + travelDisutility.getLinkTravelDisutility(toLink, toLinkEnterTime, null, null);

            return new VrpPathImpl(departureTime, (int)accTT, cost, links, accLinkTravelTimes);
        }
        else {
            return new VrpPathImpl(departureTime, 0, 0, new Link[] { fromLink }, new int[] { 0 });
        }
    }
}
