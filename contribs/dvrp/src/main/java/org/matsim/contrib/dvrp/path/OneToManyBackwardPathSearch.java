/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;


public class OneToManyBackwardPathSearch
    extends AbstractOneToManyPathSearch
{
    public OneToManyBackwardPathSearch(BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra)
    {
        super(backwardMultiNodeDijkstra);
    }


    /**
     * This is backward search, so the meaning of variables is inverted.</br>
     * 
     * @param fromLink destination
     * @param toLink origin
     * @param startTime arrivalTime
     */
    @Override
    protected PathData createPathData(Link from, Link to, double time)
    {
        if (to == from) {
            //we are already there, so let's use fromNode instead of toNode
            return new PathData(to.getFromNode(), 0.);
        }
        else {
            double delay = VrpPaths.FIRST_LINK_TT + VrpPaths.getLastLinkTT(from, time);
            return new PathData(to.getToNode(), delay);
        }
    }


    @Override
    protected Node getFromNode(Link fromLink)
    {
        return fromLink.getFromNode();
    }
}
