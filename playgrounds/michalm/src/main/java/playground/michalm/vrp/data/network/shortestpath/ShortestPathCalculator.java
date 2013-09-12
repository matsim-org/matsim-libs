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

package playground.michalm.vrp.data.network.shortestpath;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.vrp.data.network.MatsimVertex;


public class ShortestPathCalculator
{
    private final LeastCostPathCalculator router;
    private final TravelTime travelTime;
    private final TravelDisutility travelDisutility;


    public ShortestPathCalculator(LeastCostPathCalculator router, TravelTime travelTime,
            TravelDisutility travelDisutility)
    {
        this.router = router;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
    }


    public ShortestPath calculateShortestPath(MatsimVertex fromVertex, MatsimVertex tovVertex,
            int departTime)
    {
        return calculateShortestPath(fromVertex.getLink(), tovVertex.getLink(), departTime);
    }

    
    /**
     * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode()) 
     */
    public ShortestPath calculateShortestPath(Link fromLink, Link toLink, int departureTime)
    {
        if (fromLink != toLink) {
            Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departureTime, null, null);

            int count = path.links.size();
            Id[] ids = new Id[count + 2];
            int[] accLinkTravelTimes = new int[count + 2];

            ids[0] = fromLink.getId();
            double accTT = 1.;//we start at the end of fromLink
            //actually, in QSim, it usually takes 1 second to move over the first node
            //(when INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES is ON;
            //otherwise it may take even much longer)
            accLinkTravelTimes[0] = (int)accTT;

            for (int i = 0; i < count; i++) {
                Link link = path.links.get(i);
                ids[i + 1] = link.getId();
                accTT += travelTime.getLinkTravelTime(link, departureTime + accTT, null, null);
                accLinkTravelTimes[i + 1] = (int)accTT;
            }

            ids[count + 1] = toLink.getId();
            int toLinkEnterTime = departureTime + (int)accTT;
            accTT += travelTime.getLinkTravelTime(toLink, toLinkEnterTime, null, null);
            accLinkTravelTimes[count + 1] = (int)accTT;

            double cost = path.travelCost
                    + travelDisutility.getLinkTravelDisutility(toLink, toLinkEnterTime, null, null);

            return new ShortestPath((int)accTT, cost, ids, accLinkTravelTimes);
        }
        else {
            return new ShortestPath(0, 0, new Id[] { fromLink.getId() }, new int[] { 0 });
        }
    }
}
