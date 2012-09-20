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

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.*;


/**
 * @author michalm
 */
public class SparseMatsimArc
    extends AbstractMatsimArc
{
    private final ShortestPathCalculator shortestPathCalculator;
    private final TimeDiscretizer timeDiscretizer;

    private final Link fromLink;
    private final Link toLink;

    private ShortestPath[] shortestPaths = null;// lazy initialization


    public SparseMatsimArc(ShortestPathCalculator shortestPathCalculator,
            TimeDiscretizer timeDiscretizer, MatsimVertex fromVertex, MatsimVertex toVertex)
    {
        this.shortestPathCalculator = shortestPathCalculator;
        this.timeDiscretizer = timeDiscretizer;

        fromLink = fromVertex.getLink();
        toLink = toVertex.getLink();
    }


    @Override
    public ShortestPath getShortestPath(int departTime)
    {
        // lazy initialization of the SP entries
        if (shortestPaths == null) {
            shortestPaths = new ShortestPath[timeDiscretizer.getIntervalCount()];
        }

        int idx = timeDiscretizer.getIdx(departTime);
        ShortestPath shortestPath = shortestPaths[idx];

        // loads necessary data on demand
        if (shortestPath == null) {
            shortestPath = shortestPaths[idx] = shortestPathCalculator.calculateShortestPath(
                    fromLink, toLink, timeDiscretizer.getTime(idx));
        }

        return shortestPath;
    }


    public static class SparseMatsimArcFactory
        implements ArcFactory
    {
        private final ShortestPathCalculator shortestPathCalculator;
        private final TimeDiscretizer timeDiscretizer;


        public SparseMatsimArcFactory(ShortestPathCalculator shortestPathCalculator,
                TimeDiscretizer timeDiscretizer)
        {
            this.shortestPathCalculator = shortestPathCalculator;
            this.timeDiscretizer = timeDiscretizer;
        }


        @Override
        public Arc createArc(Vertex fromVertex, Vertex toVertex)
        {
            return new SparseMatsimArc(shortestPathCalculator, timeDiscretizer,
                    (MatsimVertex)fromVertex, (MatsimVertex)toVertex);
        }
    }
}
