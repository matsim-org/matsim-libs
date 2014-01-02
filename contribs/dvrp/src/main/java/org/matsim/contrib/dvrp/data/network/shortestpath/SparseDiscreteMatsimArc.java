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
import org.matsim.contrib.dvrp.data.network.AbstractMatsimArc;

import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.util.TimeDiscretizer;


/**
 * @author michalm
 */
public class SparseDiscreteMatsimArc
    extends AbstractMatsimArc
{
    private final ShortestPathCalculator shortestPathCalculator;
    private final TimeDiscretizer timeDiscretizer;

    private ShortestPath[] shortestPaths = null;// lazy initialization


    public SparseDiscreteMatsimArc(Link fromLink, Link toLink,
            ShortestPathCalculator shortestPathCalculator, TimeDiscretizer timeDiscretizer)
    {
        super(fromLink, toLink);
        this.shortestPathCalculator = shortestPathCalculator;
        this.timeDiscretizer = timeDiscretizer;
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


    public static class SparseDiscreteMatsimArcFactory
        implements ArcFactory
    {
        private final ShortestPathCalculator shortestPathCalculator;
        private final TimeDiscretizer timeDiscretizer;


        public SparseDiscreteMatsimArcFactory(ShortestPathCalculator shortestPathCalculator,
                TimeDiscretizer timeDiscretizer)
        {
            this.shortestPathCalculator = shortestPathCalculator;
            this.timeDiscretizer = timeDiscretizer;
        }


        @Override
        public Arc createArc(Link fromLink, Link toLink)
        {
            return new SparseDiscreteMatsimArc(fromLink, toLink, shortestPathCalculator,
                    timeDiscretizer);
        }
    }
}
