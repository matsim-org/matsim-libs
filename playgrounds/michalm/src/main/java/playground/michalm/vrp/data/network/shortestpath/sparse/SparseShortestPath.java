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

package playground.michalm.vrp.data.network.shortestpath.sparse;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;


public class SparseShortestPath
    implements ShortestPath
{
    private final SparseShortestPathFinder sspFinder;

    private final Link fromLink;
    private final Link toLink;

    private SPEntry[] entries = null;// lazy initialization


    public SparseShortestPath(SparseShortestPathFinder sspFinder, MatsimVertex fromVertex,
            MatsimVertex toVertex)
    {
        this.sspFinder = sspFinder;
        fromLink = fromVertex.getLink();
        toLink = toVertex.getLink();
    }


    private int getIdx(int departTime)
    {
        int idx = (departTime / sspFinder.TIME_BIN_SIZE);
        return sspFinder.CYCLIC ? (idx % sspFinder.NUM_SLOTS) : idx;
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        // lazy initialization of the SP entries
        if (entries == null) {
            entries = new SPEntry[sspFinder.NUM_SLOTS];
        }

        int idx = getIdx(departTime);
        SPEntry entry = entries[idx];

        // loads necessary data on demand
        if (entry == null) {
            entry = entries[idx] = calculateSPEntry(departTime);
        }

        return entry;
    }


    private SPEntry calculateSPEntry(int departTime)
    {
        if (fromLink != toLink) {
            Path path = sspFinder.router.calcLeastCostPath(fromLink.getToNode(),
                    toLink.getFromNode(), departTime, null, null);

            double time = path.travelTime;
            double cost = path.travelCost;
            int idCount = path.links.size() + 1;
            Id[] ids = new Id[idCount];
            int idxShift;

            if (ShortestPath.INCLUDE_TO_LINK) {
                time += sspFinder.travelTime.getLinkTravelTime(toLink, departTime, null, null);
                cost += sspFinder.travelCost.getLinkTravelDisutility(toLink, time, null, null);
                ids[idCount - 1] = toLink.getId();
                idxShift = 0;
            }
            else {
                time += sspFinder.travelTime.getLinkTravelTime(fromLink, departTime, null, null);
                cost += sspFinder.travelCost.getLinkTravelDisutility(fromLink, time, null, null);
                ids[0] = fromLink.getId();
                idxShift = 1;
            }

            for (int idx = 0; idx < idCount - 1; idx++) {
                ids[idx + idxShift] = path.links.get(idx).getId();
            }

            return new SPEntry((int)time, cost, ids);
        }
        else {
            return ShortestPath.ZERO_PATH_ENTRY;
        }
    }
}
