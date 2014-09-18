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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.utils.LeastCostPathTree;
import org.matsim.utils.LeastCostPathTree.NodeData;


public class LeastCostPathTreeStorage
{
    private final Network network;
    private final LeastCostPathTree leastCostPathTree;
    private final Map<Id<Node>, Map<Id<Node>, NodeData>> treesByRootNodeId;


    public LeastCostPathTreeStorage(Network network)
    {
        this.network = network;

        TravelTime travelTime = new FreeSpeedTravelTime();
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        leastCostPathTree = new LeastCostPathTree(travelTime, travelDisutility);

        treesByRootNodeId = new HashMap<>();
    }


    public Map<Id<Node>, NodeData> getTree(Link fromLink)
    {
        return getTree(fromLink.getToNode());
    }


    public Map<Id<Node>, NodeData> getTree(Node rootNode)
    {
        Map<Id<Node>, NodeData> tree = treesByRootNodeId.get(rootNode.getId());

        if (tree != null) {
            return tree;
        }

        leastCostPathTree.calculate(network, rootNode, 0);
        tree = leastCostPathTree.getTree();
        treesByRootNodeId.put(rootNode.getId(), tree);
        return tree;
    }
}
