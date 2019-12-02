package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public final class Dijkstra extends DijkstraImpl {
	protected Dijkstra( Network network, TravelDisutility costFunction, TravelTime timeFunction ){
		super( network, costFunction, timeFunction );
	}
	protected Dijkstra( Network network, TravelDisutility costFunction, TravelTime timeFunction, PreProcessDijkstra preProcessData ){
		super( network, costFunction, timeFunction, preProcessData );
	}
}
