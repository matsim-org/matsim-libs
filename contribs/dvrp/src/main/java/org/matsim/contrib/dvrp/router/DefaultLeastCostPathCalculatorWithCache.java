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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.*;


public class DefaultLeastCostPathCalculatorWithCache
    implements LeastCostPathCalculatorWithCache
{
    private final LeastCostPathCalculator calculator;
    private final TimeDiscretizer timeDiscretizer;
    private final Table<Id<Node>, Id<Node>, Path>[] pathCache;

    private CacheStats cacheStats = new CacheStats();


    @SuppressWarnings("unchecked")
    public DefaultLeastCostPathCalculatorWithCache(LeastCostPathCalculator calculator,
            TimeDiscretizer timeDiscretizer)
    {
        this.calculator = calculator;
        this.timeDiscretizer = timeDiscretizer;

        pathCache = new Table[timeDiscretizer.getIntervalCount()];
        for (int i = 0; i < pathCache.length; i++) {
            pathCache[i] = HashBasedTable.create();
        }
    }


    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, Person person,
            Vehicle vehicle)
    {
        Table<Id<Node>, Id<Node>, Path> spCacheSlice = pathCache[timeDiscretizer.getIdx(startTime)];
        Path path = spCacheSlice.get(fromNode.getId(), toNode.getId());

        if (path == null) {
            cacheStats.incMisses();
            path = calculator.calcLeastCostPath(fromNode, toNode,
                    timeDiscretizer.discretize(startTime), person, vehicle);
            spCacheSlice.put(fromNode.getId(), toNode.getId(), path);
        }
        else {
            cacheStats.incHits();
        }

        return path;
    }


    @Override
    public CacheStats getCacheStats()
    {
        return cacheStats;
    }
}
