package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
public class SpeedyDijkstraFactory implements LeastCostPathCalculatorFactory {

	private final Map<Network, SpeedyGraph> graphs = new ConcurrentHashMap<>();

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network, TravelDisutility travelCosts, TravelTime travelTimes) {
		SpeedyGraph graph = graphs.get(network);
		if (graph == null) {
			graph = SpeedyGraphBuilder.build(network);
			graphs.put(network, graph);
		}
		return new SpeedyDijkstra(graph, travelTimes, travelCosts);
	}
}
