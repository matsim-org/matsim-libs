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

import playground.michalm.vrp.data.network.*;


public class PreciseMatsimArc
    extends AbstractMatsimArc
{
    private final ShortestPathCalculator shortestPathCalculator;

    private final Link fromLink;
    private final Link toLink;


    public PreciseMatsimArc(ShortestPathCalculator shortestPathCalculator, MatsimVertex fromVertex,
            MatsimVertex toVertex)
    {
        this.shortestPathCalculator = shortestPathCalculator;

        fromLink = fromVertex.getLink();
        toLink = toVertex.getLink();
    }


    @Override
    public ShortestPath getShortestPath(int departTime)
    {
        return shortestPathCalculator.calculateShortestPath(fromLink, toLink, departTime);
    }
}
