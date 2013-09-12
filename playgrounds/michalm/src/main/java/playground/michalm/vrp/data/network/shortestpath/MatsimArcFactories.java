/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.data.network.shortestpath;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;


public class MatsimArcFactories
{
    public static ArcFactory createArcFactory(Network network, TravelTime travelTime,
            TravelDisutility travelDisutility, TimeDiscretizer timeDiscretizer, boolean preciseArc)
    {
        LeastCostPathCalculator router = new Dijkstra(network, travelDisutility, travelTime);
        ShortestPathCalculator shortestPathCalculator = new ShortestPathCalculator(router,
                travelTime, travelDisutility);

        return preciseArc
                ? //
                new PreciseMatsimArc.PreciseMatsimArcFactory(shortestPathCalculator)
                : new SparseDiscreteMatsimArc.SparseDiscreteMatsimArcFactory(
                        shortestPathCalculator, timeDiscretizer);
    }

}
