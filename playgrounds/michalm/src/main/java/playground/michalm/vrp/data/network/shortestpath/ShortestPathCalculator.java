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


public class ShortestPathCalculator
{
    // include toLink or fromLink in time/cost (depends on the way the qsim is implemented...)
    // by default: true (toLinks are included)
    public static final boolean INCLUDE_TO_LINK = true;

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


    public ShortestPath calculateShortestPath(Link fromLink, Link toLink, int departTime)
    {
        if (fromLink != toLink) {
            Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departTime, null, null);

            double time = path.travelTime;
            double cost = path.travelCost;
            int idCount = path.links.size() + 1;
            Id[] ids = new Id[idCount];
            int idxShift;

            if (INCLUDE_TO_LINK) {
                time += travelTime.getLinkTravelTime(toLink, departTime, null, null);
                cost += travelDisutility.getLinkTravelDisutility(toLink, time, null, null);
                ids[idCount - 1] = toLink.getId();
                idxShift = 0;
            }
            else {
                time += travelTime.getLinkTravelTime(fromLink, departTime, null, null);
                cost += travelDisutility.getLinkTravelDisutility(fromLink, time, null, null);
                ids[0] = fromLink.getId();
                idxShift = 1;
            }

            for (int idx = 0; idx < idCount - 1; idx++) {
                ids[idx + idxShift] = path.links.get(idx).getId();
            }

            return new ShortestPath((int)time, cost, ids);
        }
        else {
            return ShortestPath.ZERO_PATH_ENTRY;
        }
    }
}
