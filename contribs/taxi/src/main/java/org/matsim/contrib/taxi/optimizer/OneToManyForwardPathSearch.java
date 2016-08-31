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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.FastMultiNodeDijkstra;


public class OneToManyForwardPathSearch
    extends AbstractOneToManyPathSearch
{
    public OneToManyForwardPathSearch(FastMultiNodeDijkstra forwardMultiNodeDijkstra)
    {
        super(forwardMultiNodeDijkstra);
    }


    /**
     * @param from origin link
     * @param to destination link
     * @param time departure time
     */
    @Override
    protected PathData createPathData(Link from, Link to, double time)
    {
        if (to == from) {
            //we are already there, so let's use toNode instead of fromNode
            return new PathData(to.getToNode(), 0);
        }
        else {
            //simplified, but works for taxis, since empty drives are short (about 5 mins)
            //TODO delay can be computed more accurately after path search...
            double delay = VrpPaths.FIRST_LINK_TT + VrpPaths.getLastLinkTT(to, time);
            return new PathData(to.getFromNode(), delay);
        }
    }


    @Override
    protected Node getFromNode(Link fromLink)
    {
        return fromLink.getToNode();
    }
}
