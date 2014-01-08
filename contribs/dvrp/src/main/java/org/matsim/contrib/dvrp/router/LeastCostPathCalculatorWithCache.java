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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;


public class LeastCostPathCalculatorWithCache
    implements LeastCostPathCalculator
{
    private final LeastCostPathCalculator calculator;

    private final TimeDiscretizer timeDiscretizer;

    private final Map<Id, Map<Id, Path>>[] pathCache;


    @SuppressWarnings("unchecked")
    public LeastCostPathCalculatorWithCache(LeastCostPathCalculator calculator,
            TimeDiscretizer timeDiscretizer)
    {
        this.calculator = calculator;
        this.timeDiscretizer = timeDiscretizer;

        pathCache = new Map[timeDiscretizer.getIntervalCount()];

        for (int i = 0; i < pathCache.length; i++) {
            pathCache[i] = new HashMap<Id, Map<Id, Path>>();
        }
    }


    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person,
            Vehicle vehicle)
    {
        Map<Id, Map<Id, Path>> spCacheSlice = pathCache[timeDiscretizer.getIdx((int)starttime)];

        Map<Id, Path> spCacheFromNode = spCacheSlice.get(fromNode.getId());
        Path path = null;

        if (spCacheFromNode == null) {
            spCacheFromNode = new HashMap<Id, Path>();
            spCacheSlice.put(fromNode.getId(), spCacheFromNode);
        }
        else {
            path = spCacheFromNode.get(toNode.getId());
        }

        if (path == null) {
            path = calculator.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
            spCacheFromNode.put(toNode.getId(), path);
        }

        return path;
    }
}
