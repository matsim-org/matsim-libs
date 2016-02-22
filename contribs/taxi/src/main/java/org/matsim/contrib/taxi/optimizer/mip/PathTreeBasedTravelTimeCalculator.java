/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.mip;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.router.DijkstraWithDijkstraTreeCache;


public class PathTreeBasedTravelTimeCalculator
{
    private final DijkstraWithDijkstraTreeCache dijkstraTrees;


    public PathTreeBasedTravelTimeCalculator(DijkstraWithDijkstraTreeCache dijkstraTrees)
    {
        this.dijkstraTrees = dijkstraTrees;
    }


    public double calcTravelTime(Link fromLink, Link toLink)
    {
        if (fromLink == toLink) {
            return 0;
        }

        double tt = 1;//getting over the first node
        tt += dijkstraTrees.getTree(fromLink.getToNode(), 0).getTime(toLink.getFromNode());//travelling along the path
        tt += toLink.getLength() / toLink.getFreespeed();//travelling the last link (approx.)
        return tt;
    }
}
