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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.locationchoice.router.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.core.router.*;
import org.matsim.core.router.util.RoutingNetwork;


public class AssignmentTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final MultiNodeDijkstra router;
    private final BackwardFastMultiNodeDijkstra backwardRouter;


    public AssignmentTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        super(optimContext, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true);

        router = new MultiNodeDijkstra(optimContext.context.getScenario().getNetwork(),
                optimContext.travelDisutility, optimContext.travelTime, true);

        FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
        RoutingNetwork routingNetwork = new InverseArrayRoutingNetworkFactory(null)
                .createRoutingNetwork(optimContext.context.getScenario().getNetwork());
        backwardRouter = new BackwardFastMultiNodeDijkstra(routingNetwork,
                optimContext.travelDisutility, optimContext.travelTime, null, fastRouterFactory,
                true);
    }


    protected void scheduleUnplannedRequests()
    {
        new AssignmentProblem(optimContext, router, backwardRouter)
                .scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);
    }
}
