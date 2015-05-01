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

package playground.michalm.zone.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.DistanceUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.michalm.zone.*;


public class ZoneDistances
{
    public static final ZoneDistance BEELINE_DISTANCE_CALCULATOR = new ZoneDistance() {
        @Override
        public double calcDistance(Zone fromZone, Zone toZone)
        {
            Coord fromCoord = Zones.getCentroidCoord(fromZone);
            Coord toCoord = Zones.getCentroidCoord(toZone);
            return DistanceUtils.calculateDistance(fromCoord, toCoord);
        }
    };


    public static ZoneDistance crateFreespeedDistanceCalculator(final Network network)
    {
        return crateFreespeedBasedCalculator(network, false);
    }


    public static ZoneDistance crateFreespeedTimeCalculator(final Network network)
    {
        return crateFreespeedBasedCalculator(network, true);
    }


    private static ZoneDistance crateFreespeedBasedCalculator(final Network network,
            final boolean timeBased)
    {
        return new ZoneDistance() {
            private final LeastCostPathCalculator pathCalc = createPathCalculator(network,
                    timeBased);


            @Override
            public double calcDistance(Zone fromZone, Zone toZone)
            {
                NetworkImpl networkImpl = (NetworkImpl)network;
                Node fromNode = networkImpl.getNearestNode(Zones.getCentroidCoord(fromZone));
                Node toNode = networkImpl.getNearestNode(Zones.getCentroidCoord(toZone));
                return pathCalc.calcLeastCostPath(fromNode, toNode, 0, null, null).travelCost;
            }
        };
    }


    private static LeastCostPathCalculator createPathCalculator(Network network, boolean timeBased)
    {
        TravelTime ttimeCalc = new FreeSpeedTravelTime();
        TravelDisutility tcostCalc = timeBased ? new TimeAsTravelDisutility(ttimeCalc)
                : new DistanceAsTravelDisutility();
        return new Dijkstra(network, tcostCalc, ttimeCalc);
    }
}
