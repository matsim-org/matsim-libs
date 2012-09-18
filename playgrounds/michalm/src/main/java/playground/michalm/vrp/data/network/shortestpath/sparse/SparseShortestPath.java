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

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.util.lang.TimeDiscretizer;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.shortestpath.*;


public class SparseShortestPath
    implements ShortestPath
{
    private final ShortestPathCalculator shortestPathCalculator;
    private final TimeDiscretizer timeDiscretizer;

    private final Link fromLink;
    private final Link toLink;

    private SPEntry[] entries = null;// lazy initialization


    public SparseShortestPath(ShortestPathCalculator shortestPathCalculator,
            TimeDiscretizer timeDiscretizer, MatsimVertex fromVertex, MatsimVertex toVertex)
    {
        this.shortestPathCalculator = shortestPathCalculator;
        this.timeDiscretizer = timeDiscretizer;

        fromLink = fromVertex.getLink();
        toLink = toVertex.getLink();
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        // lazy initialization of the SP entries
        if (entries == null) {
            entries = new SPEntry[timeDiscretizer.getIntervalCount()];
        }

        int idx = timeDiscretizer.getIdx(departTime);
        SPEntry entry = entries[idx];

        // loads necessary data on demand
        if (entry == null) {
            entry = entries[idx] = shortestPathCalculator.calculateSPEntry(fromLink, toLink,
                    departTime);
        }

        return entry;
    }
}
