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
import org.matsim.core.router.util.*;


public class AssignmentTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    private final AssignmentTaxiOptimizerParams params;
    private final FastMultiNodeDijkstra router;
    private final BackwardFastMultiNodeDijkstra backwardRouter;


    public AssignmentTaxiOptimizer(TaxiOptimizerContext optimContext,
            AssignmentTaxiOptimizerParams params)
    {
        super(optimContext, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true);
        this.params = params;

        //TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
        //PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
        //preProcessDijkstra.run(optimContext.network);
        PreProcessDijkstra preProcessDijkstra = null;
        FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

        RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessDijkstra)
                .createRoutingNetwork(optimContext.network);
        router = new FastMultiNodeDijkstra(routingNetwork, optimContext.travelDisutility,
                optimContext.travelTime, preProcessDijkstra, fastRouterFactory, true);

        RoutingNetwork inverseRoutingNetwork = new InverseArrayRoutingNetworkFactory(
                preProcessDijkstra).createRoutingNetwork(optimContext.network);
        backwardRouter = new BackwardFastMultiNodeDijkstra(inverseRoutingNetwork,
                optimContext.travelDisutility, optimContext.travelTime, preProcessDijkstra,
                fastRouterFactory, true);
    }


    protected void scheduleUnplannedRequests()
    {
        new AssignmentProblem(optimContext, params, router, backwardRouter)
                .scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);
    }
}
