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
import org.matsim.contrib.dvrp.data.network.AbstractArc;

import pl.poznan.put.vrp.dynamic.data.network.*;


public class PreciseArc
    extends AbstractArc
{
    private final ShortestPathCalculator shortestPathCalculator;


    public PreciseArc(Link fromLink, Link toLink, ShortestPathCalculator shortestPathCalculator)
    {
        super(fromLink, toLink);
        this.shortestPathCalculator = shortestPathCalculator;
    }


    @Override
    public ShortestPath getShortestPath(int departTime)
    {
        return shortestPathCalculator.calculateShortestPath(fromLink, toLink, departTime);
    }


    public static class PreciseArcFactory
        implements ArcFactory
    {
        private final ShortestPathCalculator shortestPathCalculator;


        public PreciseArcFactory(ShortestPathCalculator shortestPathCalculator)
        {
            this.shortestPathCalculator = shortestPathCalculator;
        }


        @Override
        public Arc createArc(Link fromLink, Link toLink)
        {
            return new PreciseArc(fromLink, toLink, shortestPathCalculator);
        }
    }

}
