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

package org.matsim.contrib.dvrp.router;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.Maps;


public class DijkstraWithDijkstraTreeCache
    implements LeastCostPathCalculatorWithCache
{
    private final Network network;
    private final TravelDisutility costFunction;
    private final TravelTime timeFunction;
    private final TimeDiscretizer timeDiscretizer;
    private final Map<Id<Node>, DijkstraTree>[] treeCache;

    private CacheStats cacheStats = new CacheStats();


    @SuppressWarnings("unchecked")
    public DijkstraWithDijkstraTreeCache(Network network, TravelDisutility costFunction,
            final TravelTime timeFunction, TimeDiscretizer timeDiscretizer)
    {
        this.network = network;
        this.costFunction = costFunction;
        this.timeFunction = timeFunction;
        this.timeDiscretizer = timeDiscretizer;

        treeCache = new Map[timeDiscretizer.getIntervalCount()];
        for (int i = 0; i < treeCache.length; i++) {
            treeCache[i] = Maps.newHashMap();
        }
    }


    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, Person person,
            Vehicle vehicle)
    {
        return getTree(fromNode, startTime).calcLeastCostPath(fromNode, toNode,
                timeDiscretizer.discretize(startTime), person, vehicle);
    }


    public DijkstraTree getTree(Node fromNode, double startTime)
    {
        Map<Id<Node>, DijkstraTree> treeCacheSlice = treeCache[timeDiscretizer.getIdx(startTime)];
        DijkstraTree tree = treeCacheSlice.get(fromNode.getId());

        if (tree == null) {
            cacheStats.incMisses();
            tree = new DijkstraTree(network, costFunction, timeFunction);
            tree.calcLeastCostPathTree(fromNode, timeDiscretizer.discretize(startTime));
            treeCacheSlice.put(fromNode.getId(), tree);
        }
        else {
            cacheStats.incHits();
        }

        return tree;
    }


    @Override
    public CacheStats getCacheStats()
    {
        return cacheStats;
    }
}
