package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public final class MultiNodeDijkstra extends MultiNodeDijkstraImpl {
        MultiNodeDijkstra( Network network, TravelDisutility costFunction, TravelTime timeFunction, boolean searchAllEndNodes ){
                super( network, costFunction, timeFunction, searchAllEndNodes );
        }
        MultiNodeDijkstra( Network network, TravelDisutility costFunction, TravelTime timeFunction, PreProcessDijkstra preProcessData, boolean searchAllEndNodes ){
                super( network, costFunction, timeFunction, preProcessData, searchAllEndNodes );
        }
}
