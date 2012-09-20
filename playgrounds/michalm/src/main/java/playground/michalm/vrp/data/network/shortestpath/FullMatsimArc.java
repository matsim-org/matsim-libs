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


public class FullMatsimArc
    extends AbstractMatsimArc
{
    private final TimeDiscretizer timeDiscretizer;
    private final ShortestPath[] shortestPaths;


    public FullMatsimArc(TimeDiscretizer timeDiscretizer, ShortestPath[] shortestPaths)
    {
        this.timeDiscretizer = timeDiscretizer;
        this.shortestPaths = shortestPaths;
    }


    @Override
    public ShortestPath getShortestPath(int departTime)
    {
        return shortestPaths[timeDiscretizer.getIdx(departTime)];
    }


    /*package*/ShortestPath[] getShortestPaths()
    {
        return shortestPaths;
    }


    public static class FullMatsimArcFactory
        implements ArcFactory
    {
        private final ShortestPathCalculator shortestPathCalculator;
        private final TimeDiscretizer timeDiscretizer;


        public FullMatsimArcFactory(ShortestPathCalculator shortestPathCalculator,
                TimeDiscretizer timeDiscretizer)
        {
            this.shortestPathCalculator = shortestPathCalculator;
            this.timeDiscretizer = timeDiscretizer;
        }


        @Override
        public Arc createArc(Vertex fromVertex, Vertex toVertex)
        {
            ShortestPath[] shortestPaths = new ShortestPath[timeDiscretizer.getIntervalCount()];
            Link fromLink = ((MatsimVertex)fromVertex).getLink();
            Link toLink = ((MatsimVertex)toVertex).getLink();
            int timeInterval = timeDiscretizer.getTimeInterval();

            for (int k = 0; k < shortestPaths.length; k++) {
                int departTime = k * timeInterval;// + travelTimeBinSize/2 TODO
                shortestPaths[k] = shortestPathCalculator.calculateShortestPath(fromLink, toLink,
                        departTime);
            }

            return new FullMatsimArc(timeDiscretizer, shortestPaths);
        }
    }
}
