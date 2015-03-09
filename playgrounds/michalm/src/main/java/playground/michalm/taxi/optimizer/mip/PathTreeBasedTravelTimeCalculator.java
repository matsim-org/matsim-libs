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

package playground.michalm.taxi.optimizer.mip;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree.NodeData;


public class PathTreeBasedTravelTimeCalculator
{
    private final LeastCostPathTreeStorage leastCostPathTrees;


    public PathTreeBasedTravelTimeCalculator(LeastCostPathTreeStorage leastCostPathTrees)
    {
        this.leastCostPathTrees = leastCostPathTrees;
    }


    public double calcTravelTime(Link fromLink, Link toLink)
    {
        if (fromLink == toLink) {
            return 0;
        }

        Map<Id<Node>, NodeData> tree = leastCostPathTrees.getTree(fromLink);
        NodeData nodeData = tree.get(toLink.getFromNode().getId());

        double tt = 1;//getting over the first node
        tt += nodeData.getTime();//travelling along the path
        tt += toLink.getLength() / toLink.getFreespeed();//travelling the last link (approx.)

        return tt;
    }
}
