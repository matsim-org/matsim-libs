package org.matsim.core.router;

import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public final class FastMultiNodeDijkstra extends FastMultiNodeDijkstraImpl {
        protected FastMultiNodeDijkstra( RoutingNetwork routingNetwork, TravelDisutility costFunction, TravelTime timeFunction,
                                         PreProcessDijkstra preProcessData, FastRouterDelegateFactory fastRouterFactory, boolean searchAllEndNodes ){
                super( routingNetwork, costFunction, timeFunction, preProcessData, fastRouterFactory, searchAllEndNodes );
        }
}
