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

import playground.michalm.vrp.data.network.*;


public class PreciseMatsimArc
    extends AbstractMatsimArc
{
    private final ShortestPathCalculator shortestPathCalculator;


    public PreciseMatsimArc(MatsimVertex fromVertex, MatsimVertex toVertex,
            ShortestPathCalculator shortestPathCalculator)
    {
        super(fromVertex, toVertex);
        this.shortestPathCalculator = shortestPathCalculator;
    }


    @Override
    public ShortestPath getShortestPath(int departTime)
    {
        return shortestPathCalculator.calculateShortestPath(fromVertex.getLink(),
                toVertex.getLink(), departTime);
    }
}
