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

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.shortestpath.*;


/**
 * TODO The current implementation is simplistic; the class will be re-implemented in the future.
 * 
 * @author michalm
 */
public class SparseShortestPathArc
    implements ShortestPathArc
{
    private final ShortestPath shortestPath;


    public SparseShortestPathArc(ShortestPath shortestPath)
    {
        this.shortestPath = shortestPath;
    }


    @Override
    public int getTimeOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelTime;
    }


    @Override
    public int getTimeOnArrival(int arrivalTime)
    {
        // TODO: very rough!!!
        return shortestPath.getSPEntry(arrivalTime).travelTime;

        // probably a bit more accurate but still rough and more time consuming
        // return shortestPath.getSPEntry(arrivalTime -
        // shortestPath.getSPEntry(arrivalTime).travelTime);
    }


    @Override
    public double getCostOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelCost;
    }


    @Override
    public ShortestPath getShortestPath()
    {
        return shortestPath;
    }


    public static class SparseShortestPathArcBuilder
        implements ArcBuilder
    {
        private final ShortestPathCalculator shortestPathCalculator;
        private final TimeDiscretizer timeDiscretizer;

        private Vertex vertexFrom;
        private Vertex vertexTo;


        public SparseShortestPathArcBuilder(ShortestPathCalculator shortestPathCalculator,
                TimeDiscretizer timeDiscretizer)
        {
            this.shortestPathCalculator = shortestPathCalculator;
            this.timeDiscretizer = timeDiscretizer;
        }


        @Override
        public ArcBuilder setVertexFrom(Vertex vertexFrom)
        {
            this.vertexFrom = vertexFrom;
            return this;
        }


        @Override
        public ArcBuilder setVertexTo(Vertex vertexTo)
        {
            this.vertexTo = vertexTo;
            return this;
        }


        @Override
        public Arc build()
        {
            return new SparseShortestPathArc(new SparseShortestPath(shortestPathCalculator,
                    timeDiscretizer, (MatsimVertex)vertexFrom, (MatsimVertex)vertexTo));
        }
    }
}
