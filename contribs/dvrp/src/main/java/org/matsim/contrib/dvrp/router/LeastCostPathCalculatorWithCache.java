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


public class LeastCostPathCalculatorWithCache
    implements LeastCostPathCalculator
{
    private final LeastCostPathCalculator calculator;
    private final TimeDiscretizer timeDiscretizer;
    private final Table<Id<Node>, Id<Node>, Path>[] pathCache;

    private int cacheHits = 0;
    private int cacheMisses = 0;


    @SuppressWarnings("unchecked")
    public LeastCostPathCalculatorWithCache(LeastCostPathCalculator calculator,
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
            cacheMisses++;
            path = calculator.calcLeastCostPath(fromNode, toNode,
                    timeDiscretizer.discretize(startTime), person, vehicle);
            spCacheSlice.put(fromNode.getId(), toNode.getId(), path);
        }
        else {
            cacheHits++;
        }

        return path;
    }


    public int getCacheHits()
    {
        return cacheHits;
    }


    public int getCacheMisses()
    {
        return cacheMisses;
    }
}
