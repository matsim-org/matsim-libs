/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.distance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


public class DistanceCalculators
{
    public static final DistanceCalculator BEELINE_DISTANCE_CALCULATOR = new DistanceCalculator() {
        @Override
        public double calcDistance(Coord from, Coord to)
        {
            return DistanceUtils.calculateDistance(from, to);
        }
    };


    public static DistanceCalculator crateFreespeedDistanceCalculator(final Network network)
    {
        return crateFreespeedBasedCalculator(network, false);
    }


    public static DistanceCalculator crateFreespeedTimeCalculator(final Network network)
    {
        return crateFreespeedBasedCalculator(network, true);
    }


    private static DistanceCalculator crateFreespeedBasedCalculator(final Network network,
            final boolean timeBased)
    {
        TravelTime ttimeCalc = new FreeSpeedTravelTime();
        TravelDisutility tcostCalc = timeBased ? new TimeAsTravelDisutility(ttimeCalc)
                : new DistanceAsTravelDisutility();
        final DijkstraWithDijkstraTreeCache dijkstraTree = new DijkstraWithDijkstraTreeCache(
                network, tcostCalc, ttimeCalc, TimeDiscretizer.CYCLIC_24_HOURS);

        return new DistanceCalculator() {
            @Override
            public double calcDistance(Coord from, Coord to)
            {
                NetworkImpl networkImpl = (NetworkImpl)network;
                Node fromNode = networkImpl.getNearestNode(from);
                Node toNode = networkImpl.getNearestNode(to);
                return dijkstraTree.calcLeastCostPath(fromNode, toNode, 0, null, null).travelCost;
            }
        };
    }
}
